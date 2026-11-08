package com.vettyou.mail;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MailRules {

	static final String CLASSIFIER = "rules-1";

	private static final List<Rule> RULES = List.of(
			new Rule("rejected", List.of("we chose another candidate", "other candidate", "another candidate",
					"not moving forward", "will not be moving forward", "won't be moving forward", "not selected",
					"position has been filled", "we regret to", "unfortunately", "after careful consideration",
					"decided not to move forward", "not a match for this role", "will not be proceeding")),
			new Rule("offer", List.of("pleased to offer", "offer you the role", "job offer", "offer of employment")),
			new Rule("interview", List.of("interview is confirmed", "interview scheduled", "phone screen",
					"schedule an interview", "invite you to interview")),
			new Rule("moving_forward", List.of("next steps in the process", "move forward with your application",
					"would like to schedule", "advance in the process")),
			new Rule("applied", List.of("thank you for applying", "thanks for applying", "thank you for your application",
					"we received your application", "application has been received", "application received",
					"successfully submitted your application")));

	private MailRules() {
	}

	static Label label(String subject, String snippet) {
		String text = ((subject == null ? "" : subject) + "\n" + (snippet == null ? "" : snippet)).toLowerCase(Locale.ROOT);
		for (Rule rule : RULES) {
			for (String phrase : rule.phrases) {
				int at = text.indexOf(phrase);
				if (at >= 0) {
					return new Label(rule.category, quote(subject, snippet, phrase));
				}
			}
		}
		return new Label("unknown", "");
	}

	static String statusOf(String category) {
		return switch (category) {
			case "applied", "rejected", "moving_forward", "interview", "offer" -> category;
			default -> "applied";
		};
	}

	private static String quote(String subject, String snippet, String phrase) {
		String body = snippet == null || snippet.isBlank() ? subject : snippet;
		if (body == null) {
			return phrase;
		}
		Matcher sentence = Pattern.compile("(?i)[^.!?\\n]*" + Pattern.quote(phrase) + "[^.!?\\n]*[.!?]?")
			.matcher(body);
		if (sentence.find()) {
			return sentence.group().strip();
		}
		int at = body.toLowerCase(Locale.ROOT).indexOf(phrase);
		if (at < 0) {
			return phrase;
		}
		int start = Math.max(0, at - 40);
		int end = Math.min(body.length(), at + phrase.length() + 80);
		return body.substring(start, end).strip();
	}

	record Label(String category, String quote) {
	}

	private record Rule(String category, List<String> phrases) {
	}

}
