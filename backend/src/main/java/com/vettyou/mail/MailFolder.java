package com.vettyou.mail;

import java.util.Locale;

final class MailFolder {

	private MailFolder() {
	}

	static String section(String folder) {
		String leaf = leaf(folder);
		if (isListing(leaf, folder)) {
			return "listings";
		}
		if (leaf.contains("reject")) {
			return "rejections";
		}
		if (leaf.equals("wins") || leaf.equals("win")) {
			return "wins";
		}
		if (leaf.contains("application") && (leaf.contains("receiv") || leaf.contains("reciev"))) {
			return "received";
		}
		return "other";
	}

	static boolean shouldSync(String folder) {
		String value = folder == null ? "" : folder;
		String leaf = leaf(value);
		if (leaf.contains("trash") || leaf.contains("spam") || leaf.contains("junk") || leaf.contains("draft")
				|| leaf.contains("archive")) {
			return false;
		}
		return !"listings".equals(section(value));
	}

	static MailRules.Label label(String folder, String subject, String snippet) {
		MailRules.Label phrase = MailRules.label(subject, snippet);
		return switch (section(folder)) {
			case "received" -> new MailRules.Label("applied", firstQuote(phrase, subject));
			case "rejections" -> new MailRules.Label("rejected", firstQuote(phrase, subject));
			case "wins" -> "offer".equals(phrase.category()) || "interview".equals(phrase.category()) ? phrase
					: new MailRules.Label("moving_forward", firstQuote(phrase, subject));
			default -> phrase;
		};
	}

	private static String firstQuote(MailRules.Label phrase, String subject) {
		if (phrase.quote() != null && !phrase.quote().isBlank()) {
			return phrase.quote();
		}
		return subject == null ? "" : subject;
	}

	private static boolean isListing(String leaf, String folder) {
		String path = folder == null ? "" : folder.toLowerCase(Locale.ROOT);
		return leaf.contains("listing") || leaf.contains("glassdoor") || leaf.equals("indeed") || leaf.equals("monster")
				|| path.contains("job listing");
	}

	private static String leaf(String folder) {
		if (folder == null || folder.isBlank()) {
			return "";
		}
		String value = folder.replace('\\', '/').toLowerCase(Locale.ROOT);
		int slash = Math.max(value.lastIndexOf('.'), value.lastIndexOf('/'));
		return slash < 0 ? value.strip() : value.substring(slash + 1).strip();
	}

}
