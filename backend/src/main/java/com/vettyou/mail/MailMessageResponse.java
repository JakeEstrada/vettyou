package com.vettyou.mail;

import java.time.Instant;
import java.util.UUID;

public record MailMessageResponse(
		UUID id,
		Instant sentAt,
		String fromName,
		String fromAddress,
		String subject,
		String snippet,
		String quote,
		String folder,
		String section,
		String category,
		String reviewState,
		UUID proposedApplicationId,
		String proposedTitle,
		String proposedCompany) {

	static MailMessageResponse from(MailMessage message, String proposedTitle, String proposedCompany) {
		return new MailMessageResponse(message.getId(), message.getSentAt(), message.getFromName(),
				message.getFromAddress(), message.getSubject(), MailMatch.clip(message.getSnippet(), 400),
				message.getQuote(), message.getFolder(), MailFolder.section(message.getFolder()), message.getCategory(),
				message.getReviewState(), message.getProposedApplicationId(), proposedTitle == null ? "" : proposedTitle,
				proposedCompany == null ? "" : proposedCompany);
	}

}
