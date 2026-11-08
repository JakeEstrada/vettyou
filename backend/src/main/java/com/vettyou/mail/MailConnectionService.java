package com.vettyou.mail;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vettyou.common.InvalidFieldsException;

@Service
public class MailConnectionService {

	static final String DEFAULT_HOST = "mail.privateemail.com";

	static final int DEFAULT_PORT = 993;

	private final MailConnectionRepository connections;

	private final MailMessageRepository messages;

	private final MailCursorRepository cursors;

	private final MailLinkRepository links;

	private final ImapGateway imap;

	private final SecretBox secrets;

	public MailConnectionService(MailConnectionRepository connections, MailMessageRepository messages,
			MailCursorRepository cursors, MailLinkRepository links, ImapGateway imap, SecretBox secrets) {
		this.connections = connections;
		this.messages = messages;
		this.cursors = cursors;
		this.links = links;
		this.imap = imap;
		this.secrets = secrets;
	}

	@Transactional(readOnly = true)
	public MailConnectionResponse get(UUID userId) {
		return connections.findByUserId(userId).map(MailConnectionResponse::of).orElseGet(MailConnectionResponse::none);
	}

	@Transactional
	public MailConnectionResponse connect(UUID userId, MailConnectionRequest request) {
		if (!secrets.ready()) {
			throw new MailNotReadyException();
		}
		String email = request.email().trim().toLowerCase();
		String host = request.host() == null || request.host().isBlank() ? DEFAULT_HOST : request.host().trim();
		int port = request.port() == null ? DEFAULT_PORT : request.port();
		if (port < 1 || port > 65535) {
			throw new InvalidFieldsException("port", "Use an IMAP port between 1 and 65535.");
		}
		try {
			imap.login(host, port, email, request.password());
		}
		catch (MailAuthException ex) {
			throw new InvalidFieldsException("password", ex.getMessage());
		}
		catch (MailReachException ex) {
			throw new InvalidFieldsException("host", ex.getMessage());
		}
		String secret = secrets.seal(request.password());
		MailConnection connection = connections.findByUserId(userId)
			.map((existing) -> {
				existing.replace(email, host, port, secret);
				return existing;
			})
			.orElseGet(() -> MailConnection.connect(userId, email, host, port, secret));
		return MailConnectionResponse.of(connections.save(connection));
	}

	@Transactional
	public void disconnect(UUID userId) {
		links.deleteByUserId(userId);
		messages.deleteByUserId(userId);
		cursors.deleteByUserId(userId);
		connections.findByUserId(userId).ifPresent(connections::delete);
	}

	MailConnection require(UUID userId) {
		return connections.findByUserId(userId).orElseThrow(com.vettyou.common.NotFoundException::new);
	}

	String password(MailConnection connection) {
		if (!secrets.ready()) {
			throw new MailNotReadyException();
		}
		return secrets.open(connection.getSecret());
	}

}
