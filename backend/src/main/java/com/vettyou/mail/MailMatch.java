package com.vettyou.mail;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

final class MailMatch {

	private static final Set<String> SKIP = Set.of("inc", "llc", "ltd", "corp", "co", "gmbh", "plc", "limited", "the",
			"and", "of", "for", "team", "job", "jobs", "career", "careers", "recruiting", "recruitment", "talent",
			"noreply", "mail", "email", "info", "hr", "staffing", "hospital", "services", "group", "company");

	private static final Pattern SPLIT = Pattern.compile("[^a-z0-9]+");

	private MailMatch() {
	}

	static Optional<Candidate> propose(List<Candidate> applications, String fromName, String fromAddress, String subject,
			String snippet) {
		String haystack = ((fromName == null ? "" : fromName) + " " + (fromAddress == null ? "" : fromAddress) + " "
				+ (subject == null ? "" : subject) + " " + (snippet == null ? "" : snippet)).toLowerCase(Locale.ROOT);
		String domain = domainLabel(fromAddress);
		Candidate best = null;
		int bestScore = 0;
		for (Candidate application : applications) {
			int score = score(application, haystack, domain);
			if (score > bestScore) {
				best = application;
				bestScore = score;
			}
		}
		return bestScore >= 4 ? Optional.of(best) : Optional.empty();
	}

	static String companyGuess(String fromName, String fromAddress) {
		String name = fromName == null ? "" : fromName.trim();
		if (!name.isBlank() && !name.toLowerCase(Locale.ROOT).contains("noreply")
				&& !name.toLowerCase(Locale.ROOT).contains("no-reply")) {
			return clip(name, 160);
		}
		String domain = fromAddress == null ? "" : fromAddress;
		int at = domain.lastIndexOf('@');
		String host = at < 0 ? domain : domain.substring(at + 1);
		return clip(host, 160);
	}

	static String titleGuess(String subject) {
		String value = subject == null ? "" : subject.trim();
		value = value.replaceFirst("(?i)^(re|fw|fwd)\\s*:\\s*", "").trim();
		value = value.replaceFirst("(?i)^thank you for (your )?application( to| for)?\\s*", "").trim();
		value = value.replaceFirst("(?i)^thanks for applying( to| for)?\\s*", "").trim();
		return clip(value, 200);
	}

	static String clip(String value, int max) {
		String text = value == null ? "" : value.trim();
		return text.length() <= max ? text : text.substring(0, max).strip();
	}

	private static int score(Candidate application, String haystack, String domain) {
		List<String> tokens = tokens(application.company());
		tokens.addAll(tokens(application.title()));
		int score = 0;
		for (String token : tokens) {
			if (token.length() >= 4 && haystack.contains(token)) {
				score = Math.max(score, token.length());
			}
		}
		if (domain.length() >= 4 && haystack.contains(domain)) {
			String company = application.company() == null ? "" : application.company().toLowerCase(Locale.ROOT);
			if (company.contains(domain)) {
				score = Math.max(score, domain.length() + 2);
			}
		}
		return score;
	}

	private static List<String> tokens(String value) {
		List<String> tokens = new ArrayList<>();
		if (value == null || value.isBlank()) {
			return tokens;
		}
		for (String part : SPLIT.split(value.toLowerCase(Locale.ROOT))) {
			if (part.length() >= 4 && !SKIP.contains(part)) {
				tokens.add(part);
			}
		}
		tokens.sort(Comparator.comparingInt(String::length).reversed());
		return tokens;
	}

	private static String domainLabel(String fromAddress) {
		if (fromAddress == null) {
			return "";
		}
		int at = fromAddress.lastIndexOf('@');
		if (at < 0 || at == fromAddress.length() - 1) {
			return "";
		}
		String host = fromAddress.substring(at + 1).toLowerCase(Locale.ROOT);
		int dot = host.indexOf('.');
		String label = dot < 0 ? host : host.substring(0, dot);
		return SKIP.contains(label) ? "" : label;
	}

	record Candidate(UUID id, String title, String company) {
	}

}
