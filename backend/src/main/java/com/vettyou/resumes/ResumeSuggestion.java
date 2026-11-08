package com.vettyou.resumes;

import java.util.UUID;

/**
 * A change to the person's own data that would make the next version better. {@code kind} is {@code space},
 * {@code too_long}, {@code confirm}, {@code skills}, or {@code readability}. {@code line}, {@code sourceKind}, and
 * {@code sourceId} point at the line and its job or project when there is one.
 */
public record ResumeSuggestion(String kind, String text, String line, String sourceKind, UUID sourceId) {
}
