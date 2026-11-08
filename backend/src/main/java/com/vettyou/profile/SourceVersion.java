package com.vettyou.profile;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "source_versions")
public class SourceVersion {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "experience_id", nullable = false)
	private UUID experienceId;

	@Column(name = "version_number", nullable = false)
	private int versionNumber;

	@Column(nullable = false)
	private String body;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected SourceVersion() {
	}

	public static SourceVersion create(UUID userId, UUID experienceId, int versionNumber, String body) {
		SourceVersion version = new SourceVersion();
		version.userId = userId;
		version.experienceId = experienceId;
		version.versionNumber = versionNumber;
		version.body = body;
		return version;
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = Instant.now();
	}

	public int getVersionNumber() {
		return versionNumber;
	}

	public String getBody() {
		return body;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
