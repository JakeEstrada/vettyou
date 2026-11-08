package com.vettyou.projects;

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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@Testcontainers
class ProjectIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JsonMapper json;

	@Test
	void readmeStackStaysCandidateUntilDecidedAndRejectedStaysRejected() throws Exception {
		MockHttpSession owner = register("owner-" + UUID.randomUUID() + "@example.com");
		MockHttpSession other = register("other-" + UUID.randomUUID() + "@example.com");

		MvcResult created = mvc.perform(post("/api/v1/projects/import").with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"fileName\":\"README.md\",\"body\":\"# Draft\\nTracks what is in my kitchen.\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Draft"))
			.andExpect(jsonPath("$.claims.length()").value(0))
			.andReturn();
		String id = idOf(created);

		mvc.perform(get("/api/v1/projects/" + id).session(other)).andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/projects").session(other))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
		mvc.perform(readme(other, id, "README.md", "Uses Java.")).andExpect(status().isNotFound());

		mvc.perform(readme(owner, id, "notes.exe", "Uses Java.")).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.fieldErrors[0].field").value("fileName"));

		MvcResult uploaded = mvc
			.perform(readme(owner, id, "README.md", "# Pantry\\nBuilt with Angular and PostgreSQL.\\n## Roadmap\\nRedis cache"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Pantry"))
			.andExpect(jsonPath("$.sources.length()").value(2))
			.andExpect(jsonPath("$.claims.length()").value(3))
			.andReturn();
		JsonNode claims = json.readTree(uploaded.getResponse().getContentAsString()).get("claims");
		String angular = claimId(claims, "Angular");
		String postgresClaim = claimId(claims, "PostgreSQL");
		for (JsonNode claim : claims) {
			if (!"candidate".equals(claim.get("status").asString())) {
				throw new AssertionError("New claims must start as candidates");
			}
			if (claim.get("name").asString().equals("Redis") && !claim.get("planned").asBoolean()) {
				throw new AssertionError("Roadmap items must be flagged as planned");
			}
			if (claim.get("name").asString().equals("Angular") && claim.get("lineNumber").asInt() != 2) {
				throw new AssertionError("Angular should come from line 2");
			}
		}

		mvc.perform(decide(other, angular, "approved")).andExpect(status().isNotFound());
		mvc.perform(decide(owner, angular, "maybe")).andExpect(status().isUnprocessableContent());
		mvc.perform(decide(owner, angular, "approved")).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("approved"));
		mvc.perform(decide(owner, postgresClaim, "rejected")).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("rejected"));

		MvcResult second = mvc
			.perform(readme(owner, id, "README.md", "Angular, PostgreSQL and Docker."))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Pantry"))
			.andExpect(jsonPath("$.sources.length()").value(3))
			.andExpect(jsonPath("$.sources[2].versionNumber").value(3))
			.andExpect(jsonPath("$.sources[1].body").value("# Pantry\nBuilt with Angular and PostgreSQL.\n## Roadmap\nRedis cache"))
			.andExpect(jsonPath("$.claims.length()").value(4))
			.andReturn();
		JsonNode after = json.readTree(second.getResponse().getContentAsString()).get("claims");
		assertStatus(after, "Angular", "approved");
		assertStatus(after, "PostgreSQL", "rejected");
		assertStatus(after, "Docker", "candidate");

		mvc.perform(put("/api/v1/projects/" + id).with(csrf())
			.session(other)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"summary\":\"Taken\"}"))
			.andExpect(status().isNotFound());
		mvc.perform(put("/api/v1/projects/" + id).with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"summary\":\"Mine\",\"repoUrl\":\"\",\"liveUrl\":\"\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Pantry"))
			.andExpect(jsonPath("$.summary").value("Mine"));
	}

	@Test
	void droppedReadmeBecomesAProjectWithStackAndBuiltList() throws Exception {
		MockHttpSession owner = register("owner-" + UUID.randomUUID() + "@example.com");
		MockHttpSession other = register("other-" + UUID.randomUUID() + "@example.com");
		String body = "# Pantry Tracker\\n\\nTracks groceries.\\n\\n## Features\\n- Barcode scanning\\n- Expiry reminders\\n\\n## Tech\\nBuilt with Angular and Spring Boot.";

		MvcResult imported = mvc.perform(post("/api/v1/projects/import").with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"fileName\":\"README.md\",\"body\":\"" + body + "\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Pantry Tracker"))
			.andExpect(jsonPath("$.summary").value("Tracks groceries."))
			.andExpect(jsonPath("$.sources.length()").value(1))
			.andReturn();
		JsonNode claims = json.readTree(imported.getResponse().getContentAsString()).get("claims");
		long skills = 0;
		long features = 0;
		for (JsonNode claim : claims) {
			if (!"candidate".equals(claim.get("status").asString())) {
				throw new AssertionError("Imported claims must start as candidates");
			}
			if ("skill".equals(claim.get("kind").asString())) {
				skills++;
			}
			if ("feature".equals(claim.get("kind").asString())) {
				features++;
			}
		}
		if (skills != 2 || features != 2) {
			throw new AssertionError("Expected 2 skills and 2 features but got " + skills + " and " + features);
		}
		assertStatus(claims, "Barcode scanning", "candidate");

		mvc.perform(get("/api/v1/projects/" + idOf(imported)).session(other)).andExpect(status().isNotFound());
		mvc.perform(post("/api/v1/projects/" + idOf(imported) + "/reread").with(csrf()).session(other))
			.andExpect(status().isNotFound());
		mvc.perform(readme(owner, idOf(imported), "README.md", "# Pantry Tracker\\n\\nNo lists here."))
			.andExpect(status().isOk());
		mvc.perform(post("/api/v1/projects/" + idOf(imported) + "/reread").with(csrf()).session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sources.length()").value(2))
			.andExpect(jsonPath("$.claims.length()").value(4));
		mvc.perform(post("/api/v1/projects/import").with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"fileName\":\"photo.png\",\"body\":\"x\"}"))
			.andExpect(status().isUnprocessableContent());
	}

	private org.springframework.test.web.servlet.RequestBuilder readme(MockHttpSession session, String id,
			String fileName, String body) {
		return post("/api/v1/projects/" + id + "/sources").with(csrf())
			.session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"fileName\":\"" + fileName + "\",\"body\":\"" + body + "\"}");
	}

	private org.springframework.test.web.servlet.RequestBuilder decide(MockHttpSession session, String claimId,
			String decision) {
		return post("/api/v1/evidence/" + claimId + "/decision").with(csrf())
			.session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"decision\":\"" + decision + "\"}");
	}

	private static String claimId(JsonNode claims, String name) {
		for (JsonNode claim : claims) {
			if (claim.get("name").asString().equals(name)) {
				return claim.get("id").asString();
			}
		}
		throw new AssertionError("No claim named " + name);
	}

	private static void assertStatus(JsonNode claims, String name, String expected) {
		for (JsonNode claim : claims) {
			if (claim.get("name").asString().equals(name)) {
				if (!claim.get("status").asString().equals(expected)) {
					throw new AssertionError(name + " should be " + expected + " but was " + claim.get("status"));
				}
				return;
			}
		}
		throw new AssertionError("No claim named " + name);
	}

	private String idOf(MvcResult result) throws Exception {
		return json.readTree(result.getResponse().getContentAsString()).get("id").asString();
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
