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
@Table(name = "mail_connections")
class MailConnection {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false)
	private String host;

	@Column(nullable = false)
	private int port;

	@Column(nullable = false)
	private String secret;

	@Column(name = "verified_at", nullable = false)
	private Instant verifiedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected MailConnection() {
	}

	static MailConnection connect(UUID userId, String email, String host, int port, String secret) {
		MailConnection connection = new MailConnection();
		connection.userId = userId;
		connection.replace(email, host, port, secret);
		return connection;
	}

	void replace(String email, String host, int port, String secret) {
		this.email = email;
		this.host = host;
		this.port = port;
		this.secret = secret;
		this.verifiedAt = Instant.now();
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

	UUID getId() {
		return id;
	}

	UUID getUserId() {
		return userId;
	}

	String getEmail() {
		return email;
	}

	String getHost() {
		return host;
	}

	int getPort() {
		return port;
	}

	String getSecret() {
		return secret;
	}

	Instant getVerifiedAt() {
		return verifiedAt;
	}

}
