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
@Table(name = "educations")
public class Education {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private String institution;

	@Column(nullable = false)
	private String credential;

	@Column(name = "field_of_study", nullable = false)
	private String fieldOfStudy;

	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Education() {
	}

	public static Education create(UUID userId, EducationRequest request) {
		Education education = new Education();
		education.userId = userId;
		education.apply(request);
		return education;
	}

	public void apply(EducationRequest request) {
		institution = request.institution().trim();
		credential = request.credential() == null ? "" : request.credential().trim();
		fieldOfStudy = request.fieldOfStudy() == null ? "" : request.fieldOfStudy().trim();
		startDate = request.startDate();
		endDate = request.endDate();
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

	public String getInstitution() {
		return institution;
	}

	public String getCredential() {
		return credential;
	}

	public String getFieldOfStudy() {
		return fieldOfStudy;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

}
