package com.vettyou.mail;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "mail_sync_cursors")
class MailCursor {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private String folder;

	@Column(nullable = false)
	private long uidvalidity;

	@Column(name = "last_uid", nullable = false)
	private long lastUid;

	@Column(name = "synced_at", nullable = false)
	private Instant syncedAt;

	protected MailCursor() {
	}

	static MailCursor start(UUID userId, String folder) {
		MailCursor cursor = new MailCursor();
		cursor.userId = userId;
		cursor.folder = folder;
		cursor.uidvalidity = 0;
		cursor.lastUid = 0;
		return cursor;
	}

	void advance(long uidvalidity, long lastUid) {
		this.uidvalidity = uidvalidity;
		this.lastUid = lastUid;
		this.syncedAt = Instant.now();
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		if (syncedAt == null) {
			syncedAt = Instant.now();
		}
	}

	@PreUpdate
	void onUpdate() {
		syncedAt = Instant.now();
	}

	long getUidvalidity() {
		return uidvalidity;
	}

	long getLastUid() {
		return lastUid;
	}

}
