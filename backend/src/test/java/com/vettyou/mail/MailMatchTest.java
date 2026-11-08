package com.vettyou.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class MailMatchTest {

	@Test
	void matchesAHospitalDomainToTheTrackedCompany() {
		MailMatch.Candidate hoag = new MailMatch.Candidate(UUID.randomUUID(), "Registered Nurse", "Hoag Hospital");
		assertThat(MailMatch.propose(List.of(hoag), "Hoag Recruiting", "jobs@hoag.org", "Thank you for applying", "")
			.map(MailMatch.Candidate::id)).contains(hoag.id());
	}

	@Test
	void doesNotMatchALinkedinDigestToATrackedJob() {
		MailMatch.Candidate hoag = new MailMatch.Candidate(UUID.randomUUID(), "Registered Nurse", "Hoag Hospital");
		assertThat(MailMatch.propose(List.of(hoag), "LinkedIn", "updates@linkedin.com", "Jobs you may like",
				"A weekly roundup of roles")).isEmpty();
	}

}
