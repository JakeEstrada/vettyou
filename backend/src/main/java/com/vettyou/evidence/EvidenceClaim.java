package com.vettyou.evidence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "evidence_claims")
public class EvidenceClaim {

	public static final String CANDIDATE = "candidate";

	public static final String APPROVED = "approved";

	public static final String REJECTED = "rejected";

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "project_id", nullable = false)
	private UUID projectId;

	@Column(name = "source_version_id", nullable = false)
	private UUID sourceVersionId;

	@Column(nullable = false)
	private String kind;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String category;

	@Column(nullable = false)
	private String quote;

	@Column(name = "line_number", nullable = false)
	private int lineNumber;

	@Column(nullable = false)
	private boolean planned;

	@Column(nullable = false)
	private String status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected EvidenceClaim() {
	}

	static EvidenceClaim skill(UUID userId, UUID projectId, UUID sourceVersionId, StackDetector.Detection detection) {
		EvidenceClaim claim = new EvidenceClaim();
		claim.userId = userId;
		claim.projectId = projectId;
		claim.sourceVersionId = sourceVersionId;
		claim.kind = "skill";
		claim.name = detection.name();
		claim.category = detection.category();
		claim.quote = detection.quote();
		claim.lineNumber = detection.lineNumber();
		claim.planned = detection.planned();
		claim.status = CANDIDATE;
		return claim;
	}

	static EvidenceClaim feature(UUID userId, UUID projectId, UUID sourceVersionId, ReadmeParser.Feature feature) {
		EvidenceClaim claim = new EvidenceClaim();
		claim.userId = userId;
		claim.projectId = projectId;
		claim.sourceVersionId = sourceVersionId;
		claim.kind = "feature";
		claim.name = feature.name();
		claim.category = feature.section();
		claim.quote = feature.quote();
		claim.lineNumber = feature.lineNumber();
		claim.planned = feature.planned();
		claim.status = CANDIDATE;
		return claim;
	}

	void decide(String decision) {
		status = decision;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getProjectId() {
		return projectId;
	}

	public UUID getSourceVersionId() {
		return sourceVersionId;
	}

	public String getKind() {
		return kind;
	}

	public String getName() {
		return name;
	}

	public String getCategory() {
		return category;
	}

	public String getQuote() {
		return quote;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public boolean isPlanned() {
		return planned;
	}

	public String getStatus() {
		return status;
	}

}
