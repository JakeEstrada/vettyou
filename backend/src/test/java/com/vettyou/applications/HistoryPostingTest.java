package com.vettyou.applications;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HistoryPostingTest {

	@Test
	void copiesNamedPayAndLeavesUnknownEmployerBlank() {
		HistoryJobRequest l3 = job("L3Harris Technologies", "Specialist Software Engineer", 97000, 180000, "year",
				"Preferred additional skills: Python or C# beneficial.", "partial verbatim excerpt");
		assertThat(HistoryPosting.skip(l3)).isFalse();
		assertThat(HistoryPosting.company(l3)).isEqualTo("L3Harris Technologies");
		assertThat(HistoryPosting.pay(l3)).isEqualTo("$97,000 – $180,000");
		assertThat(HistoryPosting.body(l3)).contains("Salary: $97,000 – $180,000")
			.contains("Preferred additional skills")
			.contains("Completeness: partial verbatim excerpt")
			.doesNotContain("invent");

		HistoryJobRequest hourly = job("Panasonic Avionics Corporation", "Software Engineer (Full Stack)", 33.17, 55.29,
				"hour", "Linux, networking, embedded hardware.", "summary");
		assertThat(HistoryPosting.pay(hourly)).isEqualTo("$33.17 – $55.29 per hour");
		assertThat(HistoryPosting.body(hourly)).contains("Hourly rate: $33.17 – $55.29 per hour");

		HistoryJobRequest unknown = job("Unknown employer", "Flight Software Engineer", 75, 100, "hour",
				"Active DoD Secret clearance required.", "summary");
		assertThat(HistoryPosting.company(unknown)).isEmpty();
		assertThat(HistoryPosting.notes(unknown)).contains("Employer was not named");
		assertThat(HistoryPosting.body(unknown)).doesNotContain("Unknown employer");
	}

	@Test
	void skipsReferenceOnlyAndMissingTitlesAndDoesNotInventPay() {
		assertThat(HistoryPosting.skip(job("TRM Labs", "Role title unavailable", null, null, "",
				"Details are not recoverable.", "reference only"))).isTrue();
		assertThat(HistoryPosting.skip(job("IBM", "", null, null, "", "Title only.", "title only"))).isTrue();
		assertThat(HistoryPosting.skip(null)).isTrue();

		HistoryJobRequest relativity = job("Relativity Space", "Full Stack Software Engineer I, Factory Platform", "",
				null, "year", "Build full-stack software tools.", "brief summary");
		assertThat(HistoryPosting.skip(relativity)).isFalse();
		assertThat(HistoryPosting.pay(relativity)).isEmpty();
		assertThat(HistoryPosting.body(relativity)).doesNotContain("Salary:").doesNotContain("Hourly rate:");
	}

	private static HistoryJobRequest job(String company, String title, Object min, Object max, String unit,
			String description, String completeness) {
		return new HistoryJobRequest(company, title, "Anaheim, CA", "On-site", min, max, "USD", unit, "", description,
				"Java; Spring", "", "conversation history", completeness);
	}

}
