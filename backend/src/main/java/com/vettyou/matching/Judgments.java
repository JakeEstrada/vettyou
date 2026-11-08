package com.vettyou.matching;

import java.util.Map;
import java.util.UUID;

/**
 * The result of one matching run, keyed by part id. {@code fingerprint} identifies the evidence lines it saw, so a
 * later change to them can be noticed.
 */
public record Judgments(String matcher, String fingerprint, Map<UUID, PartJudgment> parts) {
}
