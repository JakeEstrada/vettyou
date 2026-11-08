package com.vettyou.matching;

import java.util.UUID;

/**
 * A stored citation: the line it was taken from and the exact words used. {@code kind} is {@code job},
 * {@code project}, or {@code school}; {@code sourceId} is that job, project, or school.
 */
public record EvidenceRef(String kind, UUID sourceId, String line, String excerpt) {
}
