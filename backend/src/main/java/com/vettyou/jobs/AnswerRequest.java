package com.vettyou.jobs;

import jakarta.validation.constraints.Pattern;

/** The person's own answer for one requirement: {@code meets}, {@code gap}, or null to clear it. */
public record AnswerRequest(
		@Pattern(regexp = "meets|gap", message = "Choose meets, gap, or nothing") String answer) {
}
