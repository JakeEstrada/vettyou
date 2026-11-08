package com.vettyou.mail;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "mail_application_links")
class MailLink {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "message_id", nullable = false)
	private UUID messageId;

	@Column(name = "application_id", nullable = false)
	private UUID applicationId;

	@Column(nullable = false)
	private String match;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected MailLink() {
	}

	static MailLink accept(UUID userId, UUID messageId, UUID applicationId) {
		MailLink link = new MailLink();
		link.userId = userId;
		link.messageId = messageId;
		link.applicationId = applicationId;
		link.match = "accepted";
		return link;
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = Instant.now();
	}

}
