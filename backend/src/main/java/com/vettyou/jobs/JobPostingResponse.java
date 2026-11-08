package com.vettyou.jobs;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.vettyou.matching.MatchScore;
import com.vettyou.matching.RequirementMatch;

/**
 * A posting with its match. {@code matcher} is what judged the evidence ({@code openai:<model>}, {@code rules}, or
 * empty for postings read before evidence matching). {@code evidenceChanged} is true when saved lines changed since.
 */
public record JobPostingResponse(
		UUID id,
		String title,
		String company,
		String pay,
		String preview,
		String status,
		String reader,
		String matcher,
		Instant matchedAt,
		boolean evidenceChanged,
		String error,
		String body,
		Instant createdAt,
		List<RequirementMatch> requirements,
		MatchScore score) {
}
