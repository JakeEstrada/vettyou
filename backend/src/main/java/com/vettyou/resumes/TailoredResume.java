package com.vettyou.resumes;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * One built version of a tailored resume. Never changed after it is saved: {@code snapshot} holds everything the
 * version showed, {@code pdf} is the exact file, and {@code sourcesHash} and {@code matchedAt} tell whether the
 * person's data or the posting's match has moved on since.
 */
@Entity
@Immutable
@Table(name = "tailored_resumes")
public class TailoredResume {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "posting_id", nullable = false)
	private UUID postingId;

	@Column(nullable = false)
	private int version;

	@Column(nullable = false)
	private String template;

	@Column(nullable = false)
	private String snapshot;

	@Column(name = "plain_text", nullable = false)
	private String plainText;

	@Column(nullable = false)
	private byte[] pdf;

	@Column(name = "pdf_sha256", nullable = false)
	private String pdfSha256;

	@Column(name = "sources_hash", nullable = false)
	private String sourcesHash;

	private Integer total;

	@Column(name = "matched_at")
	private Instant matchedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected TailoredResume() {
	}

	static TailoredResume create(UUID userId, UUID postingId, int version, String template, String snapshot,
			String plainText, byte[] pdf, String pdfSha256, String sourcesHash, Integer total, Instant matchedAt) {
		TailoredResume resume = new TailoredResume();
		resume.userId = userId;
		resume.postingId = postingId;
		resume.version = version;
		resume.template = template;
		resume.snapshot = snapshot;
		resume.plainText = plainText;
		resume.pdf = pdf;
		resume.pdfSha256 = pdfSha256;
		resume.sourcesHash = sourcesHash;
		resume.total = total;
		resume.matchedAt = matchedAt;
		return resume;
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

	public UUID getPostingId() {
		return postingId;
	}

	public int getVersion() {
		return version;
	}

	public String getTemplate() {
		return template;
	}

	public String getSnapshot() {
		return snapshot;
	}

	public String getPlainText() {
		return plainText;
	}

	public byte[] getPdf() {
		return pdf;
	}

	public String getPdfSha256() {
		return pdfSha256;
	}

	public String getSourcesHash() {
		return sourcesHash;
	}

	public Integer getTotal() {
		return total;
	}

	public Instant getMatchedAt() {
		return matchedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
