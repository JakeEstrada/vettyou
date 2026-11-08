package com.vettyou.resumes;

import java.time.Instant;
import java.util.UUID;

/** One saved version in a posting's list. {@code total} is that version's Vettyou estimate, or null. */
public record TailoredResumeSummary(UUID id, int version, Instant createdAt, Integer total) {
}
