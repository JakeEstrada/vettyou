package com.vettyou.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

	@Autowired
	private MockMvc mvc;

	@Test
	void registerLoginAndMeStayWithTheSignedInUser() throws Exception {
		String first = "first-" + UUID.randomUUID() + "@example.com";
		String second = "second-" + UUID.randomUUID() + "@example.com";

		MockHttpSession firstSession = register(first, "password1");
		mvc.perform(get("/api/v1/me").session(firstSession))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(first));

		MockHttpSession secondSession = register(second, "password2");
		mvc.perform(get("/api/v1/me").session(secondSession))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(second));
		mvc.perform(get("/api/v1/me").session(firstSession))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(first));

		mvc.perform(post("/api/v1/auth/register").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(first, "password1")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));

		mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());

		mvc.perform(post("/api/v1/auth/login").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(first, "wrong-password")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		MvcResult login = mvc.perform(post("/api/v1/auth/login").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(first.toUpperCase(), "password1")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(first))
			.andReturn();
		mvc.perform(post("/api/v1/auth/logout").with(csrf()).session((MockHttpSession) login.getRequest().getSession()))
			.andExpect(status().isNoContent());
	}

	private MockHttpSession register(String email, String password) throws Exception {
		MvcResult result = mvc.perform(post("/api/v1/auth/register").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(email, password)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value(email))
			.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private static String body(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

}
