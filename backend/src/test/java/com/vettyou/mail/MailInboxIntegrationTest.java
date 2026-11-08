package com.vettyou.mail;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
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

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@Testcontainers
class MailInboxIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JsonMapper json;

	@MockitoBean
	private ImapGateway imap;

	@Test
	void syncClassifiesMailAndDoesNotChangeStatusUntilConfirmed() throws Exception {
		MockHttpSession owner = register("owner-" + UUID.randomUUID() + "@example.com");
		MockHttpSession other = register("other-" + UUID.randomUUID() + "@example.com");
		connect(owner);
		when(imap.fetch(anyString(), anyInt(), anyString(), anyString(), eq("INBOX"), anyLong(), anyLong(), anyInt()))
			.thenReturn(new ImapFetch(7, 11, List.of(
					new ImapMessage(10, "<thanks@hoag>", "jobs@hoag.org", "Hoag Recruiting", "Thank you for applying",
							Instant.parse("2026-01-02T00:00:00Z"), "We received your application for Registered Nurse."),
					new ImapMessage(11, "<no@ensign>", "noreply@ensignservices.com", "Ensign Talent",
							"Your application update", Instant.parse("2026-01-03T00:00:00Z"),
							"Unfortunately we chose another candidate for this role."))));
		when(imap.fetch(anyString(), anyInt(), anyString(), anyString(), eq("Sent"), anyLong(), anyLong(), anyInt()))
			.thenReturn(ImapFetch.empty());
		when(imap.fetch(anyString(), anyInt(), anyString(), anyString(), eq("INBOX.Sent"), anyLong(), anyLong(), anyInt()))
			.thenReturn(ImapFetch.empty());

		mvc.perform(post("/api/v1/applications").with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title":"Registered Nurse","company":"Hoag Hospital","status":"draft"}
					"""))
			.andExpect(status().isCreated());

		MvcResult synced = mvc.perform(post("/api/v1/mail/sync").with(csrf()).session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fetched").value(2))
			.andExpect(jsonPath("$.pending").value(2))
			.andExpect(jsonPath("$.messages[0].category").value("rejected"))
			.andExpect(jsonPath("$.messages[1].category").value("applied"))
			.andExpect(jsonPath("$.messages[1].proposedCompany").value("Hoag Hospital"))
			.andExpect(jsonPath("$.messages[0].folder").value("INBOX"))
			.andExpect(jsonPath("$.messages[0].section").value("other"))
			.andExpect(content().string(not(containsString("app-pass-1"))))
			.andReturn();
		String thanksId = json.readTree(synced.getResponse().getContentAsString()).get("messages").get(1).get("id")
			.asString();
		String rejectedId = json.readTree(synced.getResponse().getContentAsString()).get("messages").get(0).get("id")
			.asString();

		mvc.perform(get("/api/v1/applications").session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].status").value("draft"));

		mvc.perform(get("/api/v1/mail/messages").session(other))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
		mvc.perform(post("/api/v1/mail/messages/" + thanksId + "/accept").with(csrf()).session(other))
			.andExpect(status().isNotFound());

		mvc.perform(post("/api/v1/mail/messages/" + thanksId + "/accept").with(csrf()).session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("applied"))
			.andExpect(jsonPath("$.company").value("Hoag Hospital"));

		mvc.perform(post("/api/v1/mail/messages/" + rejectedId + "/accept").with(csrf()).session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("rejected"))
			.andExpect(jsonPath("$.company").value("Ensign Talent"));

		mvc.perform(get("/api/v1/mail/messages").session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
		mvc.perform(get("/api/v1/applications").session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void usesMailboxFoldersAndSkipsJobListingAds() throws Exception {
		MockHttpSession owner = register("folders-" + UUID.randomUUID() + "@example.com");
		connect(owner);
		when(imap.listFolders(anyString(), anyInt(), anyString(), anyString())).thenReturn(List.of("INBOX",
				"INBOX.Applications Recieved", "INBOX.Rejections", "INBOX.WINS", "INBOX.Job Listings",
				"INBOX.Job Listings.Indeed", "INBOX.Job Listings.Glassdoor Jobs", "INBOX.Job Listings.Monster"));
		when(imap.fetch(anyString(), anyInt(), anyString(), anyString(), anyString(), anyLong(), anyLong(), anyInt()))
			.thenReturn(ImapFetch.empty());
		when(imap.fetch(anyString(), anyInt(), anyString(), anyString(), eq("INBOX.Applications Recieved"), anyLong(),
				anyLong(), anyInt()))
			.thenReturn(new ImapFetch(3, 4, List.of(new ImapMessage(4, "<thanks@hoag>", "jobs@hoag.org", "Hoag Recruiting",
					"Thank you for applying", Instant.parse("2026-01-02T00:00:00Z"),
					"We received your application for Registered Nurse."))));
		when(imap.fetch(anyString(), anyInt(), anyString(), anyString(), eq("INBOX.Rejections"), anyLong(), anyLong(),
				anyInt()))
			.thenReturn(new ImapFetch(5, 6, List.of(new ImapMessage(6, "<no@ensign>", "noreply@ensignservices.com",
					"Ensign Talent", "Your application update", Instant.parse("2026-01-03T00:00:00Z"),
					"Unfortunately we chose another candidate for this role."))));
		when(imap.fetch(anyString(), anyInt(), anyString(), anyString(), eq("INBOX.Job Listings"), anyLong(), anyLong(),
				anyInt()))
			.thenReturn(new ImapFetch(9, 12, List.of(new ImapMessage(12, "<jobs@honeywell>", "noreply@honeywell.com",
					"Human Resources", "New job opportunities at Honeywell", Instant.parse("2026-01-04T00:00:00Z"),
					"We have new jobs that might interest you."))));

		mvc.perform(post("/api/v1/mail/sync").with(csrf()).session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fetched").value(2))
			.andExpect(jsonPath("$.pending").value(2))
			.andExpect(jsonPath("$.messages[0].section").value("rejections"))
			.andExpect(jsonPath("$.messages[0].folder").value("INBOX.Rejections"))
			.andExpect(jsonPath("$.messages[1].section").value("received"))
			.andExpect(jsonPath("$.messages[1].folder").value("INBOX.Applications Recieved"))
			.andExpect(content().string(not(containsString("Honeywell"))));

		verify(imap, never()).fetch(anyString(), anyInt(), anyString(), anyString(), eq("INBOX.Job Listings"), anyLong(),
				anyLong(), anyInt());
		verify(imap, never()).fetch(anyString(), anyInt(), anyString(), anyString(), eq("INBOX.Job Listings.Indeed"),
				anyLong(), anyLong(), anyInt());
	}

	private void connect(MockHttpSession session) throws Exception {
		mvc.perform(post("/api/v1/mail/connections").with(csrf())
			.session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"email":"inbox@example.com","password":"app-pass-1"}
					"""))
			.andExpect(status().isCreated());
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
