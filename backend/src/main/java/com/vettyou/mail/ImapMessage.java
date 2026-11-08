package com.vettyou.mail;

import java.time.Instant;

public record ImapMessage(
		long uid,
		String messageId,
		String fromAddress,
		String fromName,
		String subject,
		Instant sentAt,
		String snippet) {
}
