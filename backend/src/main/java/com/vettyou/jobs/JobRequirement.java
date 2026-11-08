package com.vettyou.jobs;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_requirements")
public class JobRequirement {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "posting_id", nullable = false)
	private UUID postingId;

	@Column(nullable = false)
	private int position;

	@Column(nullable = false)
	private String skill;

	@Column(nullable = false)
	private String kind;

	@Column(nullable = false)
	private String importance;

	@Column(nullable = false)
	private String category;

	@Column(nullable = false)
	private String quote;

	@Column(nullable = false)
	private String logic;

	@Column
	private String answer;

	@Column(name = "answered_at")
	private Instant answeredAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected JobRequirement() {
	}

	static JobRequirement of(UUID userId, UUID postingId, int position, JobReader.Requirement requirement) {
		JobRequirement row = new JobRequirement();
		row.userId = userId;
		row.postingId = postingId;
		row.position = position;
		row.skill = requirement.skill();
		row.kind = requirement.kind();
		row.importance = requirement.importance();
		row.category = requirement.category();
		row.quote = requirement.quote();
		row.logic = requirement.logic();
		return row;
	}

	/** {@code meets}, {@code gap}, or null to clear the answer. */
	void answer(String answer) {
		this.answer = answer;
		this.answeredAt = answer == null ? null : Instant.now();
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

	public UUID getUserId() {
		return userId;
	}

	public String getSkill() {
		return skill;
	}

	public String getKind() {
		return kind;
	}

	public String getImportance() {
		return importance;
	}

	public String getCategory() {
		return category;
	}

	public String getQuote() {
		return quote;
	}

	public String getLogic() {
		return logic;
	}

	public String getAnswer() {
		return answer;
	}

	public Instant getAnsweredAt() {
		return answeredAt;
	}

}
