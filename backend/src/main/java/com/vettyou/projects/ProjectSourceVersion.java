package com.vettyou.projects;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_source_versions")
public class ProjectSourceVersion {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "project_id", nullable = false)
	private UUID projectId;

	@Column(name = "version_number", nullable = false)
	private int versionNumber;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(nullable = false)
	private String body;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ProjectSourceVersion() {
	}

	static ProjectSourceVersion of(UUID userId, UUID projectId, int versionNumber, String fileName, String body) {
		ProjectSourceVersion version = new ProjectSourceVersion();
		version.userId = userId;
		version.projectId = projectId;
		version.versionNumber = versionNumber;
		version.fileName = fileName;
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

	public UUID getId() {
		return id;
	}

	public int getVersionNumber() {
		return versionNumber;
	}

	public String getFileName() {
		return fileName;
	}

	public String getBody() {
		return body;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
