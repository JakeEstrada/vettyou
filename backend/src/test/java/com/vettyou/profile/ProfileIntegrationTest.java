package com.vettyou.profile;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@Testcontainers
class ProfileIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JsonMapper json;

	@Test
	void profileAndExperienceStayWithTheOwnerAndKeepTheOriginalWords() throws Exception {
		MockHttpSession owner = register("owner-" + UUID.randomUUID() + "@example.com");
		MockHttpSession other = register("other-" + UUID.randomUUID() + "@example.com");

		mvc.perform(get("/api/v1/profile").session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value(""));

		mvc.perform(put("/api/v1/profile").with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"displayName":"Jake Estrada","phone":"","location":"Seattle","website":"","linkedin":""}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("Jake Estrada"))
			.andExpect(jsonPath("$.location").value("Seattle"));

		mvc.perform(get("/api/v1/profile").session(other))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value(""));

		MvcResult created = mvc.perform(post("/api/v1/experiences").with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content(job("I took over sales and grew the crew from 0 to 10.")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.versions[0].number").value(1))
			.andExpect(jsonPath("$.versions[0].body").value("I took over sales and grew the crew from 0 to 10."))
			.andReturn();
		String id = json.readTree(created.getResponse().getContentAsString()).get("id").asString();

		mvc.perform(get("/api/v1/experiences/" + id).session(other)).andExpect(status().isNotFound());

		mvc.perform(put("/api/v1/experiences/" + id).with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content(job("I took over sales and grew the crew from 0 people to 10.")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.body").value("I took over sales and grew the crew from 0 people to 10."))
			.andExpect(jsonPath("$.versions.length()").value(2))
			.andExpect(jsonPath("$.versions[0].body").value("I took over sales and grew the crew from 0 to 10."));

		mvc.perform(put("/api/v1/experiences/" + id).with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"employer":"Northwind","title":"Lead","location":"","startDate":"2022-05-01","endDate":"2020-01-01","current":false,"body":"Same words."}
					"""))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.fieldErrors[0].field").value("endDate"));
	}

	private MockHttpSession register(String email) throws Exception {
		MvcResult result = mvc.perform(post("/api/v1/auth/register").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"" + email + "\",\"password\":\"password1\"}"))
			.andExpect(status().isCreated())
			.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private static String job(String body) {
		return """
				{"employer":"Northwind","title":"Lead","location":"Seattle","startDate":"2020-01-01","endDate":null,"current":false,"body":"%s"}
				""".formatted(body);
	}

}
