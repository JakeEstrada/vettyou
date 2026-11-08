package com.vettyou.evidence;

import java.util.UUID;

public record ClaimResponse(
		UUID id,
		String kind,
		String name,
		String category,
		String quote,
		int lineNumber,
		UUID sourceVersionId,
		boolean planned,
		String status) {

	static ClaimResponse from(EvidenceClaim claim) {
		return new ClaimResponse(claim.getId(), claim.getKind(), claim.getName(), claim.getCategory(), claim.getQuote(),
				claim.getLineNumber(), claim.getSourceVersionId(), claim.isPlanned(), claim.getStatus());
	}

}
