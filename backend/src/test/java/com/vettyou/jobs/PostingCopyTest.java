package com.vettyou.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PostingCopyTest {

	@Test
	void usesTheRoleLineWhenTheModelLeftTheTitleEmpty() {
		String body = """
				Ensign Services, Inc.
				San Juan Capistrano, CA

				About the job
				Registered Nurse - Medical/Surgical Unit

				We are looking for a nurse to join a medical-surgical unit in San Juan Capistrano.
				Pay: $38.00 - $45.00 per hour
				""";

		assertThat(PostingCopy.title("", "Ensign Services, Inc.", body))
			.isEqualTo("Registered Nurse - Medical/Surgical Unit");
		assertThat(PostingCopy.pay(body)).isEqualTo("$38.00 - $45.00 per hour");
		assertThat(PostingCopy.preview(body, "Registered Nurse - Medical/Surgical Unit", "Ensign Services, Inc."))
			.startsWith("We are looking for a nurse")
			.doesNotContain("Untitled");
	}

	@Test
	void prefersAPositionLabelOverATitleThatIsTheCompany() {
		String body = """
				Title: Crossing Hurdles
				Position: Strategic Project Lead, Coding/SWE
				""";

		assertThat(PostingCopy.title("Crossing Hurdles", "Crossing Hurdles", body))
			.isEqualTo("Strategic Project Lead, Coding/SWE");
		assertThat(PostingCopy.company("", body)).isEqualTo("Crossing Hurdles");
		assertThat(PostingCopy.preview(body, "Strategic Project Lead, Coding/SWE", "Crossing Hurdles")).isEmpty();
	}

	@Test
	void keepsAStoredRoleAndLeavesPayEmptyWhenThePostingDoesNotNameIt() {
		assertThat(PostingCopy.title("Frontend Engineer", "Acme", "Frontend Engineer\nAcme\nBuild the UI."))
			.isEqualTo("Frontend Engineer");
		assertThat(PostingCopy.pay("Build the UI.")).isEmpty();
	}

	@Test
	void previewSkipsAMarkdownTitle() {
		String body = """
				# Specialist Software Engineer

				Preferred additional skills: Python or C# beneficial.

				Salary: $97,000 – $180,000
				""";
		assertThat(PostingCopy.preview(body, "Specialist Software Engineer", "L3Harris Technologies"))
			.startsWith("Preferred additional skills")
			.doesNotContain("# Specialist")
			.doesNotContain("$97,000");
	}

}
