package com.vettyou.jobs;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.vettyou.matching.EvidenceRef;

/** A citation the matcher made for a part, after Java found the excerpt word for word in that line. */
@Entity
@Table(name = "job_part_evidence")
public class JobPartEvidence {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "part_id", nullable = false)
	private UUID partId;

	@Column(nullable = false)
	private int position;

	@Column(name = "source_kind", nullable = false)
	private String sourceKind;

	@Column(name = "source_id", nullable = false)
	private UUID sourceId;

	@Column(nullable = false)
	private String line;

	@Column(nullable = false)
	private String excerpt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected JobPartEvidence() {
	}

	static JobPartEvidence of(UUID userId, UUID partId, int position, EvidenceRef ref) {
		JobPartEvidence evidence = new JobPartEvidence();
		evidence.userId = userId;
		evidence.partId = partId;
		evidence.position = position;
		evidence.sourceKind = ref.kind();
		evidence.sourceId = ref.sourceId();
		evidence.line = ref.line();
		evidence.excerpt = ref.excerpt();
		return evidence;
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = Instant.now();
	}

	public UUID getPartId() {
		return partId;
	}

	EvidenceRef ref() {
		return new EvidenceRef(sourceKind, sourceId, line, excerpt);
	}

}
