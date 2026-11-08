package com.vettyou.applications;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "applications")
public class Application {

	static final Set<String> STATUSES = Set.of("draft", "applied", "rejected", "moving_forward", "interview", "offer",
			"withdrawn", "no_reply");

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "posting_id")
	private UUID postingId;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String company;

	@Column(nullable = false)
	private String location;

	@Column(nullable = false)
	private String pay;

	@Column(nullable = false)
	private String status;

	@Column(name = "applied_at")
	private Instant appliedAt;

	@Column(nullable = false)
	private String notes;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Application() {
	}

	static Application create(UUID userId, ApplicationRequest request) {
		Application application = new Application();
		application.userId = userId;
		application.apply(request, true);
		return application;
	}

	void apply(ApplicationRequest request, boolean creating) {
		title = request.title().trim();
		company = blank(request.company());
		location = blank(request.location());
		pay = request.pay() == null && !creating ? pay : blank(request.pay());
		status = creating ? statusOf(request.status(), "draft") : statusOf(request.status(), status);
		postingId = request.postingId();
		appliedAt = request.appliedAt();
		notes = blank(request.notes());
	}

	void markFromMail(String nextStatus, Instant mailAt) {
		status = nextStatus;
		if ("applied".equals(nextStatus) && appliedAt == null) {
			appliedAt = mailAt;
		}
	}

	static Application fromMail(UUID userId, String title, String company, String status, Instant mailAt, String notes) {
		Application application = new Application();
		application.userId = userId;
		application.title = title.trim();
		application.company = blank(company);
		application.location = "";
		application.pay = "";
		application.status = status;
		application.postingId = null;
		application.appliedAt = "applied".equals(status) ? mailAt : null;
		application.notes = blank(notes);
		return application;
	}

	static String statusOf(String value, String fallback) {
		String status = value == null || value.isBlank() ? fallback : value.trim();
		if (!STATUSES.contains(status)) {
			return null;
		}
		return status;
	}

	private static String blank(String value) {
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

	public UUID getUserId() {
		return userId;
	}

	public UUID getPostingId() {
		return postingId;
	}

	public String getTitle() {
		return title;
	}

	public String getCompany() {
		return company;
	}

	public String getLocation() {
		return location;
	}

	public String getPay() {
		return pay;
	}

	public String getStatus() {
		return status;
	}

	public Instant getAppliedAt() {
		return appliedAt;
	}

	public String getNotes() {
		return notes;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
