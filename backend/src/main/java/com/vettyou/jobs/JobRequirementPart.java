package com.vettyou.jobs;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** One thing a requirement asks for, with the matcher's judgment once it has run. */
@Entity
@Table(name = "job_requirement_parts")
public class JobRequirementPart {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "requirement_id", nullable = false)
	private UUID requirementId;

	@Column(nullable = false)
	private int position;

	@Column(nullable = false)
	private String text;

	@Column(nullable = false)
	private String concept;

	@Column
	private String support;

	@Column(nullable = false)
	private String reason;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected JobRequirementPart() {
	}

	static JobRequirementPart of(UUID userId, UUID requirementId, int position, String text, String concept) {
		JobRequirementPart part = new JobRequirementPart();
		part.userId = userId;
		part.requirementId = requirementId;
		part.position = position;
		part.text = text;
		part.concept = concept;
		part.reason = "";
		return part;
	}

	void judge(String support, String reason) {
		this.support = support;
		this.reason = reason;
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getRequirementId() {
		return requirementId;
	}

	public String getText() {
		return text;
	}

	public String getConcept() {
		return concept;
	}

	public String getSupport() {
		return support;
	}

	public String getReason() {
		return reason;
	}

}
