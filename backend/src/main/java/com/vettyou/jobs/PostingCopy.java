package com.vettyou.jobs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Words copied from a pasted posting for lists and applications. Empty when the posting does not name them. Never
 * invents a title, company, or amount.
 */
final class PostingCopy {

	private static final Pattern ROLE_LABEL = Pattern
		.compile("(?im)^\\s*(?:position|role|job title)\\s*:\\s*(\\S.{0,118})$");

	private static final Pattern TITLE_LABEL = Pattern.compile("(?im)^\\s*title\\s*:\\s*(\\S.{0,118})$");

	private static final Pattern PAY_LABEL = Pattern.compile(
			"(?im)^\\s*(?:base\\s+)?(?:salary|compensation|pay(?:\\s+range)?|hourly(?:\\s+rate)?|annual(?:\\s+(?:salary|pay|compensation))?)\\s*:\\s*(.{1,80})$");

	private static final Pattern SKIP_LINE = Pattern.compile(
			"(?i)^(about(?: the (?:job|role|company|team|us))?|job description|overview|who we are|what we do|our mission|description|remote|on-?site|hybrid|full[ -]?time|part[ -]?time|.+,\\s*[A-Z]{2}\\s*)$");

	private static final Pattern SKIP_LABELED = Pattern.compile(
			"(?i)^(title|position|role|job title|company|employer|salary|compensation|pay(?:\\s+range)?|hourly(?:\\s+rate)?|annual(?:\\s+(?:salary|pay))?|type|job type|location)\\s*:");

	private static final Pattern SECTION = Pattern.compile(
			"(?i)\\b(requirements?|qualifications?|responsibilities|nice[ -]to[ -]have|preferred|bonus|benefits|perks|"
					+ "about (?:us|the (?:company|team|role|job))|who you are|what you(?:'ll| will)? (?:do|bring|need)|"
					+ "you have|skills|tech stack|compensation|equal opportunity|our stack|duties|day[ -]to[ -]day|"
					+ "application process|how to apply)\\b");

	private static final Pattern BULLET = Pattern.compile("^\\s*(?:[-*+•●▪◦]|\\d+[.)])\\s+");

	private static final int TITLE_LIMIT = 80;

	private static final int PREVIEW_LIMIT = 220;

	private PostingCopy() {
	}

	static String title(String stored, String company, String body) {
		String employer = clean(company);
		String role = labeled(ROLE_LABEL, body, employer);
		if (!role.isEmpty()) {
			return role;
		}
		String kept = clean(stored);
		if (!kept.isEmpty() && !kept.equalsIgnoreCase(employer)) {
			return kept;
		}
		String titled = labeled(TITLE_LABEL, body, employer);
		if (!titled.isEmpty()) {
			return titled;
		}
		String line = firstRoleLine(body, employer);
		if (!line.isEmpty()) {
			return line;
		}
		return firstWords(body, employer);
	}

	static String company(String stored, String body) {
		String kept = clean(stored);
		if (!kept.isEmpty()) {
			return kept;
		}
		String titled = labeled(TITLE_LABEL, body, "");
		String role = labeled(ROLE_LABEL, body, "");
		if (!titled.isEmpty() && !titled.equalsIgnoreCase(role)) {
			return titled;
		}
		return "";
	}

	static String pay(String body) {
		Matcher matcher = PAY_LABEL.matcher(body == null ? "" : body);
		while (matcher.find()) {
			String value = clip(clean(matcher.group(1)), TITLE_LIMIT);
			if (!value.isEmpty() && value.chars().anyMatch(Character::isDigit)) {
				return value;
			}
		}
		return "";
	}

	static String preview(String body, String title, String company) {
		StringBuilder text = new StringBuilder();
		for (String raw : lines(body)) {
			String line = clean(raw);
			if (line.isEmpty() || line.startsWith("#") || line.equalsIgnoreCase(clean(company))
					|| line.equalsIgnoreCase(clean(title)) || SKIP_LINE.matcher(line).matches()
					|| SKIP_LABELED.matcher(line).find()) {
				continue;
			}
			if (text.length() > 0) {
				text.append(' ');
			}
			text.append(line);
			if (text.length() >= PREVIEW_LIMIT) {
				break;
			}
		}
		return clip(text.toString(), PREVIEW_LIMIT);
	}

	private static String firstRoleLine(String body, String company) {
		for (String raw : lines(body)) {
			String line = clean(raw);
			if (line.isEmpty() || line.equalsIgnoreCase(company) || SKIP_LINE.matcher(line).matches()
					|| BULLET.matcher(line).find() || heading(line) || line.startsWith("http") || line.contains("@")
					|| line.length() > 100 || line.split("\\s+").length > 14) {
				continue;
			}
			return clip(line, TITLE_LIMIT);
		}
		return "";
	}

	private static String firstWords(String body, String company) {
		for (String raw : lines(body)) {
			String line = clean(raw);
			if (line.isEmpty() || line.equalsIgnoreCase(company) || SKIP_LINE.matcher(line).matches()) {
				continue;
			}
			return clip(line, TITLE_LIMIT);
		}
		return "";
	}

	private static String labeled(Pattern label, String body, String company) {
		Matcher matcher = label.matcher(body == null ? "" : body);
		while (matcher.find()) {
			String value = clip(clean(matcher.group(1)), TITLE_LIMIT);
			if (!value.isEmpty() && !value.equalsIgnoreCase(company) && value.split(" ").length <= 12) {
				return value;
			}
		}
		return "";
	}

	private static boolean heading(String line) {
		return line.length() <= 80 && !BULLET.matcher(line).find() && line.split("\\s+").length <= 8
				&& (line.endsWith(":") || SECTION.matcher(line).find() && !line.matches(".*[.;,]$"));
	}

	private static String[] lines(String body) {
		return (body == null ? "" : body).split("\\R");
	}

	private static String clip(String text, int limit) {
		if (text.length() <= limit) {
			return text;
		}
		int cut = text.lastIndexOf(' ', limit);
		return (cut < 40 ? text.substring(0, limit) : text.substring(0, cut)).strip() + "…";
	}

	private static String clean(String text) {
		return text == null ? "" : text.replaceAll("\\s+", " ").strip();
	}

}
