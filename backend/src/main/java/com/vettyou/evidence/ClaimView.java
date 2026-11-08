package com.vettyou.evidence;

import java.util.UUID;

/** A claim as other modules see it: enough to match and cite, never the entity. */
public record ClaimView(
		UUID id,
		UUID projectId,
		String kind,
		String name,
		String category,
		String quote,
		boolean planned,
		String status) {

	static ClaimView from(EvidenceClaim claim) {
		return new ClaimView(claim.getId(), claim.getProjectId(), claim.getKind(), claim.getName(), claim.getCategory(),
				claim.getQuote(), claim.isPlanned(), claim.getStatus());
	}

	public boolean approved() {
		return EvidenceClaim.APPROVED.equals(status);
	}

	public boolean candidate() {
		return EvidenceClaim.CANDIDATE.equals(status);
	}

}
