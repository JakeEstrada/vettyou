package com.vettyou.projects;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class Project {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String summary;

	@Column(name = "repo_url", nullable = false)
	private String repoUrl;

	@Column(name = "live_url", nullable = false)
	private String liveUrl;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Project() {
	}

	static Project fromReadme(UUID userId, String name, String summary) {
		Project project = new Project();
		project.userId = userId;
		project.name = name;
		project.summary = summary;
		project.repoUrl = "";
		project.liveUrl = "";
		return project;
	}

	void rename(String name) {
		this.name = name;
	}

	void apply(ProjectDetailsRequest request) {
		summary = clean(request.summary());
		repoUrl = clean(request.repoUrl());
		liveUrl = clean(request.liveUrl());
	}

	private static String clean(String value) {
		return value == null ? "" : value.trim();
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

	public String getName() {
		return name;
	}

	public String getSummary() {
		return summary;
	}

	public String getRepoUrl() {
		return repoUrl;
	}

	public String getLiveUrl() {
		return liveUrl;
	}

}
