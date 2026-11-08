package com.vettyou.applications;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_status_events")
public class ApplicationStatusEvent {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "application_id", nullable = false)
	private UUID applicationId;

	@Column(nullable = false)
	private String status;

	@Column(nullable = false)
	private String note;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ApplicationStatusEvent() {
	}

	static ApplicationStatusEvent record(UUID userId, UUID applicationId, String status, String note) {
		ApplicationStatusEvent event = new ApplicationStatusEvent();
		event.userId = userId;
		event.applicationId = applicationId;
		event.status = status;
		event.note = note == null ? "" : note.trim();
		return event;
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public String getStatus() {
		return status;
	}

	public String getNote() {
		return note;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
