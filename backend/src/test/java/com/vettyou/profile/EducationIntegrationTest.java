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
class EducationIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JsonMapper json;

	@Test
	void educationStaysWithTheOwner() throws Exception {
		MockHttpSession owner = register("owner-" + UUID.randomUUID() + "@example.com");
		MockHttpSession other = register("other-" + UUID.randomUUID() + "@example.com");

		MvcResult created = mvc.perform(post("/api/v1/educations").with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"institution":"Bellevue College","credential":"AS","fieldOfStudy":"Computer Science","startDate":"2019-09-01","endDate":"2021-06-01"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.institution").value("Bellevue College"))
			.andReturn();
		String id = json.readTree(created.getResponse().getContentAsString()).get("id").asString();

		mvc.perform(get("/api/v1/educations").session(other))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
		mvc.perform(put("/api/v1/educations/" + id).with(csrf())
			.session(other)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"institution\":\"Stolen\"}"))
			.andExpect(status().isNotFound());

		mvc.perform(put("/api/v1/educations/" + id).with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"institution":"Bellevue College","credential":"AS","fieldOfStudy":"CS","startDate":"2021-09-01","endDate":"2020-06-01"}
					"""))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.fieldErrors[0].field").value("endDate"));

		mvc.perform(get("/api/v1/educations").session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].fieldOfStudy").value("Computer Science"));
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
