package com.vettyou.evidence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "claim_decisions")
public class ClaimDecision {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "claim_id", nullable = false)
	private UUID claimId;

	@Column(nullable = false)
	private String decision;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ClaimDecision() {
	}

	static ClaimDecision of(UUID userId, UUID claimId, String decision) {
		ClaimDecision record = new ClaimDecision();
		record.userId = userId;
		record.claimId = claimId;
		record.decision = decision;
		return record;
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = Instant.now();
	}

}
