package com.vettyou.profile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "work_experiences")
public class WorkExperience {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private String employer;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String location;

	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Column(name = "still_here", nullable = false)
	private boolean currentRole;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected WorkExperience() {
	}

	public static WorkExperience create(UUID userId, ExperienceRequest request) {
		WorkExperience experience = new WorkExperience();
		experience.userId = userId;
		experience.apply(request);
		return experience;
	}

	public void apply(ExperienceRequest request) {
		employer = request.employer().trim();
		title = request.title().trim();
		location = request.location() == null ? "" : request.location().trim();
		startDate = request.startDate();
		currentRole = request.current();
		endDate = request.current() ? null : request.endDate();
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

	public String getEmployer() {
		return employer;
	}

	public String getTitle() {
		return title;
	}

	public String getLocation() {
		return location;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public boolean isCurrentRole() {
		return currentRole;
	}

}
