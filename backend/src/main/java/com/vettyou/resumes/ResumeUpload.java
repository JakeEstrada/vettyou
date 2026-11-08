package com.vettyou.resumes;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "resume_uploads")
public class ResumeUpload {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(nullable = false)
	private String body;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ResumeUpload() {
	}

	static ResumeUpload create(UUID userId, String fileName, String body) {
		ResumeUpload upload = new ResumeUpload();
		upload.userId = userId;
		upload.fileName = fileName;
		upload.body = body;
		return upload;
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = Instant.now();
	}

}
