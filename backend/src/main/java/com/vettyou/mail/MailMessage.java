package com.vettyou.mail;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "mail_messages")
class MailMessage {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private String folder;

	@Column(nullable = false)
	private long uid;

	@Column(nullable = false)
	private long uidvalidity;

	@Column(name = "internet_message_id", nullable = false)
	private String internetMessageId;

	@Column(name = "from_address", nullable = false)
	private String fromAddress;

	@Column(name = "from_name", nullable = false)
	private String fromName;

	@Column(nullable = false)
	private String subject;

	@Column(name = "sent_at", nullable = false)
	private Instant sentAt;

	@Column(nullable = false)
	private String snippet;

	@Column(nullable = false)
	private String quote;

	@Column(nullable = false)
	private String category;

	@Column(nullable = false)
	private String classifier;

	@Column(name = "review_state", nullable = false)
	private String reviewState;

	@Column(name = "proposed_application_id")
	private UUID proposedApplicationId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected MailMessage() {
	}

	static MailMessage from(UUID userId, String folder, long uidvalidity, ImapMessage fetched, MailRules.Label label,
			UUID proposedApplicationId) {
		MailMessage message = new MailMessage();
		message.userId = userId;
		message.folder = folder;
		message.uid = fetched.uid();
		message.uidvalidity = uidvalidity;
		message.internetMessageId = fetched.messageId() == null ? "" : fetched.messageId();
		message.fromAddress = fetched.fromAddress() == null ? "" : fetched.fromAddress();
		message.fromName = fetched.fromName() == null ? "" : fetched.fromName();
		message.subject = fetched.subject() == null ? "" : fetched.subject();
		message.sentAt = fetched.sentAt() == null ? Instant.now() : fetched.sentAt();
		message.snippet = fetched.snippet() == null ? "" : fetched.snippet();
		message.quote = label.quote();
		message.category = label.category();
		message.classifier = MailRules.CLASSIFIER;
		message.reviewState = "pending";
		message.proposedApplicationId = proposedApplicationId;
		return message;
	}

	void accept() {
		reviewState = "accepted";
	}

	void ignore() {
		reviewState = "ignored";
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = Instant.now();
	}

	UUID getId() {
		return id;
	}

	UUID getUserId() {
		return userId;
	}

	String getFromAddress() {
		return fromAddress;
	}

	String getFromName() {
		return fromName;
	}

	String getSubject() {
		return subject;
	}

	Instant getSentAt() {
		return sentAt;
	}

	String getSnippet() {
		return snippet;
	}

	String getQuote() {
		return quote;
	}

	String getCategory() {
		return category;
	}

	String getReviewState() {
		return reviewState;
	}

	UUID getProposedApplicationId() {
		return proposedApplicationId;
	}

	long getUid() {
		return uid;
	}

	long getUidvalidity() {
		return uidvalidity;
	}

	String getFolder() {
		return folder;
	}

	String getInternetMessageId() {
		return internetMessageId;
	}

}
