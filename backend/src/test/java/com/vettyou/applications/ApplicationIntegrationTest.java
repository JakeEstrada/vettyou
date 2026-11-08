package com.vettyou.applications;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class ApplicationIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JsonMapper json;

	@Test
	void applicationsStayWithTheOwner() throws Exception {
		MockHttpSession owner = register("owner-" + UUID.randomUUID() + "@example.com");
		MockHttpSession other = register("other-" + UUID.randomUUID() + "@example.com");

		MvcResult created = mvc.perform(post("/api/v1/applications").with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title":"Junior engineer","company":"Acme","pay":"$120,000","status":"applied","notes":"Sent Friday"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.title").value("Junior engineer"))
			.andExpect(jsonPath("$.pay").value("$120,000"))
			.andExpect(jsonPath("$.status").value("applied"))
			.andExpect(jsonPath("$.estimate").value(nullValue()))
			.andExpect(jsonPath("$.events.length()").value(1))
			.andReturn();
		String id = json.readTree(created.getResponse().getContentAsString()).get("id").asString();

		mvc.perform(get("/api/v1/applications").session(other))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
		mvc.perform(put("/api/v1/applications/" + id).with(csrf())
			.session(other)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"Stolen\",\"status\":\"offer\"}"))
			.andExpect(status().isNotFound());
		mvc.perform(delete("/api/v1/applications/" + id).with(csrf()).session(other))
			.andExpect(status().isNotFound());

		mvc.perform(put("/api/v1/applications/" + id).with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title":"Junior engineer","company":"Acme","status":"bogus"}
					"""))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.fieldErrors[0].field").value("status"));

		mvc.perform(put("/api/v1/applications/" + id).with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title":"Junior engineer","company":"Acme","status":"interview","notes":"Phone screen Tuesday"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("interview"))
			.andExpect(jsonPath("$.events.length()").value(2));

		mvc.perform(post("/api/v1/applications").with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title":"Later role","company":"Beta","status":"draft"}
					"""))
			.andExpect(status().isCreated());

		mvc.perform(get("/api/v1/applications").session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].title").value("Later role"))
			.andExpect(jsonPath("$[1].company").value("Acme"))
			.andExpect(jsonPath("$[1].pay").value("$120,000"))
			.andExpect(jsonPath("$[1].status").value("interview"));
	}

	@Test
	void historyImportCreatesPostingsWithoutCrossingAccounts() throws Exception {
		MockHttpSession owner = register("history-owner-" + UUID.randomUUID() + "@example.com");
		MockHttpSession other = register("history-other-" + UUID.randomUUID() + "@example.com");

		MvcResult imported = mvc.perform(post("/api/v1/applications/history").with(csrf())
			.session(owner)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"jobs":[
					  {"company":"L3Harris Technologies","jobTitle":"Specialist Software Engineer","location":"Anaheim, CA","salaryMin":97000,"salaryMax":180000,"salaryCurrency":"USD","salaryUnit":"year","jobDescription":"Preferred additional skills: Python or C#.","technicalRequirements":"Safety-critical software","source":"screenshot","descriptionCompleteness":"partial verbatim excerpt"},
					  {"company":"Unknown employer","jobTitle":"Flight Software Engineer","location":"Redondo Beach, CA","salaryMin":75,"salaryMax":100,"salaryUnit":"hour","jobDescription":"Active Secret clearance required.","descriptionCompleteness":"summary"},
					  {"company":"TRM Labs","jobTitle":"Role title unavailable","descriptionCompleteness":"reference only"},
					  {"company":"L3Harris Technologies","jobTitle":"Specialist Software Engineer","salaryMin":97000,"salaryMax":180000,"salaryUnit":"year","jobDescription":"Duplicate."}
					]}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imported").value(2))
			.andExpect(jsonPath("$.skipped").value(2))
			.andExpect(jsonPath("$.applications.length()").value(2))
			.andExpect(jsonPath("$.applications[0].title").value("Specialist Software Engineer"))
			.andExpect(jsonPath("$.applications[0].company").value("L3Harris Technologies"))
			.andExpect(jsonPath("$.applications[0].pay").value("$97,000 – $180,000"))
			.andExpect(jsonPath("$.applications[0].status").value("draft"))
			.andExpect(jsonPath("$.applications[0].postingId").isNotEmpty())
			.andExpect(jsonPath("$.applications[1].company").value(""))
			.andExpect(jsonPath("$.applications[1].pay").value("$75 – $100 per hour"))
			.andReturn();

		String postingId = json.readTree(imported.getResponse().getContentAsString())
			.get("applications")
			.get(0)
			.get("postingId")
			.asString();

		mvc.perform(get("/api/v1/jobs/" + postingId).session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ready"))
			.andExpect(jsonPath("$.reader").value("history"))
			.andExpect(jsonPath("$.pay").value("$97,000 – $180,000"))
			.andExpect(jsonPath("$.score.total").value(nullValue()))
			.andExpect(jsonPath("$.body").value(containsString("Preferred additional skills")));
		mvc.perform(get("/api/v1/jobs/" + postingId).session(other)).andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/applications").session(other))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
		mvc.perform(post("/api/v1/applications/history").with(csrf())
			.session(other)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"jobs":[{"company":"L3Harris Technologies","jobTitle":"Specialist Software Engineer","salaryMin":97000,"salaryMax":180000,"salaryUnit":"year","jobDescription":"Should stay with this account."}]}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imported").value(1));
		mvc.perform(get("/api/v1/applications").session(owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2));
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
