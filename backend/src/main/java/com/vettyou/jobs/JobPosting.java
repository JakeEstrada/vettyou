package com.vettyou.jobs;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_postings")
public class JobPosting {

	public static final String READING = "reading";

	public static final String MATCHING = "matching";

	public static final String READY = "ready";

	public static final String FAILED = "failed";

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String company;

	@Column(nullable = false)
	private String body;

	@Column(nullable = false)
	private String status;

	@Column(nullable = false)
	private String reader;

	@Column(nullable = false)
	private String matcher;

	@Column(name = "evidence_hash", nullable = false)
	private String evidenceHash;

	@Column(name = "matched_at")
	private Instant matchedAt;

	@Column(nullable = false)
	private String error;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected JobPosting() {
	}

	static JobPosting create(UUID userId, String body) {
		JobPosting posting = new JobPosting();
		posting.userId = userId;
		posting.body = body;
		posting.title = "";
		posting.company = "";
		posting.reader = "";
		posting.matcher = "";
		posting.evidenceHash = "";
		posting.startReading();
		return posting;
	}

	/** Copies a saved history row as-is. Does not start a read, so nothing is invented. */
	public static JobPosting fromHistory(UUID userId, String title, String company, String body) {
		JobPosting posting = new JobPosting();
		posting.userId = userId;
		posting.body = body;
		posting.title = title;
		posting.company = company;
		posting.reader = "history";
		posting.matcher = "";
		posting.evidenceHash = "";
		posting.status = READY;
		posting.error = "";
		return posting;
	}

	void startReading() {
		status = READING;
		error = "";
	}

	void startMatching() {
		status = MATCHING;
		error = "";
	}

	/** Requirements are saved; matching still has to run, so the status stays as it is. */
	void read(String title, String company, String reader) {
		this.title = title;
		this.company = company;
		this.reader = reader;
	}

	void matched(String matcher, String evidenceHash) {
		this.matcher = matcher;
		this.evidenceHash = evidenceHash;
		matchedAt = Instant.now();
		status = READY;
		error = "";
	}

	void fail(String message) {
		status = FAILED;
		error = message;
	}

	boolean busy() {
		return READING.equals(status) || MATCHING.equals(status);
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

	public String getTitle() {
		return title;
	}

	public String getCompany() {
		return company;
	}

	public String getBody() {
		return body;
	}

	public String getStatus() {
		return status;
	}

	public String getReader() {
		return reader;
	}

	public String getMatcher() {
		return matcher;
	}

	public String getEvidenceHash() {
		return evidenceHash;
	}

	public Instant getMatchedAt() {
		return matchedAt;
	}

	public String getError() {
		return error;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
