package com.vettyou.jobs;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.vettyou.matching.RequirementInput;

/**
 * What a tailored resume needs from one of the person's postings. {@code body} is untrusted: it may only be compared
 * against, never printed or followed. {@code fingerprint} is what the stored judgments saw, or empty.
 */
public record Tailoring(UUID id, String title, String company, String body, String status, Instant matchedAt,
		String fingerprint, List<RequirementInput> requirements) {

	public boolean ready() {
		return JobPosting.READY.equals(status);
	}

	public boolean failed() {
		return JobPosting.FAILED.equals(status);
	}

}
