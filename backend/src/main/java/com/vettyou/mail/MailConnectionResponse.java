package com.vettyou.mail;

import java.time.Instant;

public record MailConnectionResponse(boolean connected, String email, String host, int port, Instant verifiedAt) {

	static MailConnectionResponse none() {
		return new MailConnectionResponse(false, "", MailConnectionService.DEFAULT_HOST, MailConnectionService.DEFAULT_PORT,
				null);
	}

	static MailConnectionResponse of(MailConnection connection) {
		return new MailConnectionResponse(true, connection.getEmail(), connection.getHost(), connection.getPort(),
				connection.getVerifiedAt());
	}

}
