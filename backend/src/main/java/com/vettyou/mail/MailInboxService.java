package com.vettyou.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.vettyou.applications.ApplicationHint;
import com.vettyou.applications.ApplicationResponse;
import com.vettyou.applications.ApplicationService;
import com.vettyou.common.InvalidFieldsException;
import com.vettyou.common.NotFoundException;

@Service
public class MailInboxService {

	static final int LIMIT = 200;

	private static final List<String> FALLBACK_FOLDERS = List.of("INBOX", "Sent", "INBOX.Sent");

	private final MailConnectionService connections;

	private final MailMessageRepository messages;

	private final MailCursorRepository cursors;

	private final MailLinkRepository links;

	private final ImapGateway imap;

	private final ApplicationService applications;

	private final TransactionTemplate tx;

	public MailInboxService(MailConnectionService connections, MailMessageRepository messages,
			MailCursorRepository cursors, MailLinkRepository links, ImapGateway imap, ApplicationService applications,
			PlatformTransactionManager transactions) {
		this.connections = connections;
		this.messages = messages;
		this.cursors = cursors;
		this.links = links;
		this.imap = imap;
		this.applications = applications;
		this.tx = new TransactionTemplate(transactions);
	}

	@Transactional(readOnly = true)
	public List<MailMessageResponse> pending(UUID userId) {
		return responses(userId, messages.findByUserIdAndReviewStateOrderBySentAtDesc(userId, "pending"));
	}

	public MailSyncResponse sync(UUID userId) {
		MailConnection connection = connections.require(userId);
		String password = connections.password(connection);
		List<MailMatch.Candidate> owned = candidatesOf(userId);
		int fetched = 0;
		for (String folder : foldersToRead(connection, password)) {
			MailCursor cursor = cursors.findByUserIdAndFolder(userId, folder)
				.orElseGet(() -> cursors.save(MailCursor.start(userId, folder)));
			ImapFetch batch = imap.fetch(connection.getHost(), connection.getPort(), connection.getEmail(), password,
					folder, cursor.getUidvalidity(), cursor.getLastUid(), LIMIT);
			if (batch.uidValidity() == 0 && batch.messages().isEmpty()) {
				continue;
			}
			Integer added = tx.execute((status) -> store(userId, folder, cursor, batch, owned));
			fetched += added == null ? 0 : added;
		}
		List<MailMessageResponse> pending = pending(userId);
		return new MailSyncResponse(fetched, pending.size(), pending);
	}

	@Transactional
	public ApplicationResponse accept(UUID userId, UUID messageId, MailAcceptRequest request) {
		MailMessage message = owned(userId, messageId);
		if (!"pending".equals(message.getReviewState())) {
			throw new InvalidFieldsException("reviewState", "This message was already reviewed.");
		}
		String status = MailRules.statusOf(message.getCategory());
		String note = message.getQuote().isBlank() ? message.getSubject() : message.getQuote();
		ApplicationResponse application;
		UUID applicationId = request == null ? null : request.applicationId();
		if (applicationId == null) {
			applicationId = message.getProposedApplicationId();
		}
		if (applicationId == null) {
			String title = MailMatch.titleGuess(message.getSubject());
			String company = MailMatch.companyGuess(message.getFromName(), message.getFromAddress());
			application = applications.createFromMail(userId, title, company, status, message.getSentAt(), note);
		}
		else {
			application = applications.markFromMail(userId, applicationId, status, message.getSentAt(), note);
		}
		links.save(MailLink.accept(userId, message.getId(), application.id()));
		message.accept();
		messages.save(message);
		return application;
	}

	@Transactional
	public void ignore(UUID userId, UUID messageId) {
		MailMessage message = owned(userId, messageId);
		message.ignore();
		messages.save(message);
	}

	private int store(UUID userId, String folder, MailCursor cursor, ImapFetch batch, List<MailMatch.Candidate> owned) {
		int fetched = 0;
		if (cursor.getUidvalidity() != 0 && cursor.getUidvalidity() != batch.uidValidity()) {
			cursor.advance(batch.uidValidity(), 0);
		}
		for (ImapMessage fetchedMessage : batch.messages()) {
			if (messages.existsByUserIdAndFolderAndUidvalidityAndUid(userId, folder, batch.uidValidity(),
					fetchedMessage.uid())) {
				continue;
			}
			MailRules.Label label = MailFolder.label(folder, fetchedMessage.subject(), fetchedMessage.snippet());
			UUID proposed = MailMatch
				.propose(owned, fetchedMessage.fromName(), fetchedMessage.fromAddress(), fetchedMessage.subject(),
						fetchedMessage.snippet())
				.map(MailMatch.Candidate::id)
				.orElse(null);
			messages.save(MailMessage.from(userId, folder, batch.uidValidity(), fetchedMessage, label, proposed));
			fetched++;
		}
		cursor.advance(batch.uidValidity(), batch.lastUid());
		cursors.save(cursor);
		return fetched;
	}

	private List<String> foldersToRead(MailConnection connection, String password) {
		List<String> listed = imap.listFolders(connection.getHost(), connection.getPort(), connection.getEmail(),
				password);
		if (listed == null || listed.isEmpty()) {
			return FALLBACK_FOLDERS;
		}
		List<String> folders = listed.stream().filter(MailFolder::shouldSync).distinct().toList();
		return folders.isEmpty() ? FALLBACK_FOLDERS : folders;
	}

	private List<MailMatch.Candidate> candidatesOf(UUID userId) {
		return applications.hints(userId)
			.stream()
			.map((hint) -> new MailMatch.Candidate(hint.id(), hint.title(), hint.company()))
			.toList();
	}

	private MailMessage owned(UUID userId, UUID messageId) {
		return messages.findByIdAndUserId(messageId, userId).orElseThrow(NotFoundException::new);
	}

	private List<MailMessageResponse> responses(UUID userId, List<MailMessage> rows) {
		Map<UUID, ApplicationHint> byId = applications.hints(userId)
			.stream()
			.collect(Collectors.toMap(ApplicationHint::id, Function.identity()));
		List<MailMessageResponse> result = new ArrayList<>();
		for (MailMessage row : rows) {
			ApplicationHint proposed = row.getProposedApplicationId() == null ? null
					: byId.get(row.getProposedApplicationId());
			String title = proposed == null ? MailMatch.titleGuess(row.getSubject()) : proposed.title();
			String company = proposed == null ? MailMatch.companyGuess(row.getFromName(), row.getFromAddress())
					: proposed.company();
			result.add(MailMessageResponse.from(row, title, company));
		}
		return result;
	}

}
