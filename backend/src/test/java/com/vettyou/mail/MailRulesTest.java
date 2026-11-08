package com.vettyou.mail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MailRulesTest {

	@Test
	void thanksForApplyingIsAppliedNotMovingForward() {
		MailRules.Label label = MailRules.label("Thank you for applying to Hoag",
				"We received your application for Registered Nurse.");
		assertThat(label.category()).isEqualTo("applied");
		assertThat(label.quote()).containsIgnoringCase("thank you for applying");
	}

	@Test
	void anotherCandidateIsRejectedEvenWhenTheyThankTheApplicant() {
		MailRules.Label label = MailRules.label("Your application",
				"Thank you for applying. Unfortunately we chose another candidate.");
		assertThat(label.category()).isEqualTo("rejected");
		assertThat(label.quote()).containsIgnoringCase("another candidate");
	}

	@Test
	void aNewsletterStaysUnknown() {
		assertThat(MailRules.label("Your weekly jobs digest", "Here are roles you might like.").category())
			.isEqualTo("unknown");
	}

}
