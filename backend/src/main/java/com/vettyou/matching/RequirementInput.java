package com.vettyou.matching;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One requirement read from a job posting, with the posting line it came from, its parts, and the person's answer
 * ({@code meets}, {@code gap}, or null).
 */
public record RequirementInput(UUID id, String skill, String kind, String importance, String category, String quote,
		String logic, String answer, Instant answeredAt, List<PartInput> parts) {
}
