package com.vettyou.mail;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@Testcontainers
class MailConnectionIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ImapGateway imap;

	@Test
	void mailboxStaysWithTheOwnerAndNeverReturnsThePassword() throws Exception {
		MockHttpSession owner = register("owner-" + UUID.randomUUID() + "@example.com");
		MockHttpSession other = register("other-" + UUID.randomUUID() + "@example.com");

		mvc.perform(get("/api/v1/mail/connection").session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.connected").value(false))
			.andExpect(jsonPath("$.password").doesNotExist());

		mvc.perform(post("/api/v1/mail/connections").with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"email":"Inbox@Example.com","password":"app-pass-1"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.connected").value(true))
			.andExpect(jsonPath("$.email").value("inbox@example.com"))
			.andExpect(jsonPath("$.host").value("mail.privateemail.com"))
			.andExpect(jsonPath("$.port").value(993))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(content().string(not(containsString("app-pass-1"))));

		verify(imap).login("mail.privateemail.com", 993, "inbox@example.com", "app-pass-1");

		mvc.perform(get("/api/v1/mail/connection").session(other))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.connected").value(false))
			.andExpect(jsonPath("$.email").value(""));

		mvc.perform(get("/api/v1/mail/connection").session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.connected").value(true))
			.andExpect(jsonPath("$.email").value("inbox@example.com"))
			.andExpect(jsonPath("$.password").doesNotExist());

		mvc.perform(delete("/api/v1/mail/connection").with(csrf()).session(other)).andExpect(status().isNoContent());
		mvc.perform(get("/api/v1/mail/connection").session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.connected").value(true));

		mvc.perform(delete("/api/v1/mail/connection").with(csrf()).session(owner)).andExpect(status().isNoContent());
		mvc.perform(get("/api/v1/mail/connection").session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.connected").value(false));
	}

	@Test
	void rejectedLoginIsNotStored() throws Exception {
		MockHttpSession owner = register("mail-" + UUID.randomUUID() + "@example.com");
		doThrow(new MailAuthException()).when(imap).login(anyString(), anyInt(), anyString(), anyString());

		mvc.perform(post("/api/v1/mail/connections").with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"email":"contact@jakeestrada.org","password":"wrong-pass"}
					"""))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
			.andExpect(content().string(not(containsString("wrong-pass"))));

		mvc.perform(get("/api/v1/mail/connection").session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.connected").value(false));
		verify(imap, never()).login(eq("mail.privateemail.com"), eq(993), eq("contact@jakeestrada.org"), eq(null));
	}

	private MockHttpSession register(String email) throws Exception {
		MvcResult result = mvc.perform(post("/api/v1/auth/register").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"" + email + "\",\"password\":\"password1\"}"))
			.andExpect(status().isCreated())
			.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

}
