package com.vettyou.matching;

import java.util.List;
import java.util.UUID;

/**
 * One thing a requirement asks for. {@code support} is {@code direct}, {@code transferable}, {@code none}, or
 * {@code unclear}, and null until judged.
 */
public record PartInput(UUID id, String text, String concept, String support, String reason,
		List<EvidenceRef> evidence) {
}
