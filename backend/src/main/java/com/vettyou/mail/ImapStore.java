package com.vettyou.mail;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.mail.Address;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.FetchProfile;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.FolderNotFoundException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;

@Component
class ImapStore implements ImapGateway {

	private static final Logger log = LoggerFactory.getLogger(ImapStore.class);

	private static final int SNIPPET = 2000;

	@Override
	public void login(String host, int port, String username, String password) {
		Store store = null;
		try {
			store = open(host, port, username, password);
		}
		finally {
			close(store);
		}
	}

	@Override
	public List<String> listFolders(String host, int port, String username, String password) {
		Store store = null;
		try {
			store = open(host, port, username, password);
			List<String> names = new ArrayList<>();
			Folder root = store.getDefaultFolder();
			add(root.list("*"), names);
			Folder inbox = store.getFolder("INBOX");
			if (inbox.exists()) {
				names.add("INBOX");
				add(inbox.list("*"), names);
			}
			return names.stream().distinct().toList();
		}
		catch (AuthenticationFailedException ex) {
			log.info("IMAP login was not accepted");
			throw new MailAuthException();
		}
		catch (MessagingException ex) {
			log.info("IMAP folders could not be listed");
			throw new MailReachException();
		}
		finally {
			close(store);
		}
	}

	@Override
	public ImapFetch fetch(String host, int port, String username, String password, String folder, long uidValidity,
			long lastUid, int limit) {
		Store store = null;
		Folder inbox = null;
		try {
			store = open(host, port, username, password);
			inbox = store.getFolder(folder);
			if (inbox == null || !inbox.exists()) {
				return ImapFetch.empty();
			}
			inbox.open(Folder.READ_ONLY);
			if (!(inbox instanceof UIDFolder uids)) {
				throw new MailReachException();
			}
			long validity = uids.getUIDValidity();
			long start = lastUid > 0 && validity == uidValidity ? lastUid + 1 : 0;
			Message[] found;
			if (start > 0) {
				found = uids.getMessagesByUID(start, UIDFolder.LASTUID);
			}
			else {
				int count = inbox.getMessageCount();
				if (count < 1) {
					return new ImapFetch(validity, 0, List.of());
				}
				int from = Math.max(1, count - Math.max(1, limit) + 1);
				found = inbox.getMessages(from, count);
			}
			FetchProfile profile = new FetchProfile();
			profile.add(FetchProfile.Item.ENVELOPE);
			profile.add(UIDFolder.FetchProfileItem.UID);
			inbox.fetch(found, profile);
			List<ImapMessage> messages = new ArrayList<>();
			long maxUid = start > 0 ? lastUid : 0;
			int kept = 0;
			for (Message message : found) {
				if (message == null || message.isExpunged() || message.isSet(Flags.Flag.DELETED)) {
					continue;
				}
				long uid = uids.getUID(message);
				if (uid <= 0) {
					continue;
				}
				maxUid = Math.max(maxUid, uid);
				if (kept >= limit) {
					continue;
				}
				messages.add(read(uid, message));
				kept++;
			}
			return new ImapFetch(validity, maxUid, List.copyOf(messages));
		}
		catch (FolderNotFoundException ex) {
			return ImapFetch.empty();
		}
		catch (AuthenticationFailedException ex) {
			log.info("IMAP login was not accepted");
			throw new MailAuthException();
		}
		catch (MessagingException ex) {
			log.info("IMAP folder could not be read");
			throw new MailReachException();
		}
		finally {
			close(inbox);
			close(store);
		}
	}

	private Store open(String host, int port, String username, String password) {
		Properties props = new Properties();
		props.put("mail.store.protocol", "imaps");
		props.put("mail.imaps.ssl.enable", "true");
		props.put("mail.imaps.ssl.checkserveridentity", "true");
		props.put("mail.imaps.connectiontimeout", "15000");
		props.put("mail.imaps.timeout", "45000");
		Store store = null;
		try {
			store = Session.getInstance(props).getStore("imaps");
			store.connect(host, port, username, password);
			return store;
		}
		catch (AuthenticationFailedException ex) {
			close(store);
			log.info("IMAP login was not accepted");
			throw new MailAuthException();
		}
		catch (MessagingException ex) {
			close(store);
			log.info("IMAP host could not be reached");
			throw new MailReachException();
		}
	}

	private static ImapMessage read(long uid, Message message) throws MessagingException {
		InternetAddress from = from(message.getFrom());
		Instant sent = message.getSentDate() == null ? Instant.now() : message.getSentDate().toInstant();
		return new ImapMessage(uid, header(message, "Message-ID"), from == null ? "" : blank(from.getAddress()),
				from == null ? "" : blank(from.getPersonal()), blank(message.getSubject()), sent,
				MailMatch.clip(text(message), SNIPPET));
	}

	private static InternetAddress from(Address[] addresses) {
		if (addresses == null || addresses.length == 0) {
			return null;
		}
		return addresses[0] instanceof InternetAddress internet ? internet : null;
	}

	private static String header(Message message, String name) throws MessagingException {
		String[] values = message.getHeader(name);
		return values == null || values.length == 0 ? "" : blank(values[0]);
	}

	private static String text(Part part) {
		try {
			if (part.isMimeType("text/plain")) {
				return string(part.getContent());
			}
			if (part.isMimeType("multipart/*")) {
				Object content = part.getContent();
				if (content instanceof Multipart multipart) {
					String html = "";
					for (int i = 0; i < multipart.getCount(); i++) {
						Part nested = multipart.getBodyPart(i);
						if (nested.isMimeType("text/plain")) {
							return string(nested.getContent());
						}
						if (html.isBlank() && nested.isMimeType("text/html")) {
							html = strip(string(nested.getContent()));
						}
						else if (nested.isMimeType("multipart/*")) {
							String nestedText = text(nested);
							if (!nestedText.isBlank()) {
								return nestedText;
							}
						}
					}
					return html;
				}
			}
			if (part.isMimeType("text/html")) {
				return strip(string(part.getContent()));
			}
		}
		catch (Exception ignored) {
			return "";
		}
		return "";
	}

	private static String string(Object content) {
		return content == null ? "" : content.toString();
	}

	private static String strip(String html) {
		return html.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("(?i)</p>", "\n").replaceAll("<[^>]+>", " ").replaceAll("\\s+",
				" ");
	}

	private static void add(Folder[] folders, List<String> names) throws MessagingException {
		if (folders == null) {
			return;
		}
		for (Folder folder : folders) {
			if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
				names.add(folder.getFullName());
			}
		}
	}

	private static String blank(String value) {
		return value == null ? "" : value.trim();
	}

	private static void close(Folder folder) {
		if (folder != null && folder.isOpen()) {
			try {
				folder.close(false);
			}
			catch (MessagingException ignored) {
				// Best-effort.
			}
		}
	}

	private static void close(Store store) {
		if (store != null && store.isConnected()) {
			try {
				store.close();
			}
			catch (MessagingException ignored) {
				// Best-effort.
			}
		}
	}

}
