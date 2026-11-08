package com.vettyou.matching;

import java.util.UUID;

/**
 * One place a skill shows up. {@code kind} is {@code project}, {@code job}, or {@code school}; {@code id} is that
 * project, job, or school. {@code status} is the claim's status, or {@code written} for the person's own words.
 */
public record SkillUse(String kind, UUID id, String label, String quote, String status) {
}
