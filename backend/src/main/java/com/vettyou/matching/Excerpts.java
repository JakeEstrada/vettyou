package com.vettyou.matching;

import java.util.Locale;

/** Word-for-word checks for quoted evidence. */
final class Excerpts {

	private static final int MIN_LENGTH = 8;

	private Excerpts() {
	}

	/** Same text, ignoring case, spacing, and curly-vs-straight quotes and dashes. */
	static String normalize(String text) {
		return text.replace('\u2018', '\'')
			.replace('\u2019', '\'')
			.replace('\u201C', '"')
			.replace('\u201D', '"')
			.replace('\u2013', '-')
			.replace('\u2014', '-')
			.replace('\u00A0', ' ')
			.replaceAll("\\s+", " ")
			.strip()
			.toLowerCase(Locale.ROOT);
	}

	/** Drops what models add around a quote: wrapping quotes, ellipses, and closing punctuation. */
	static String clean(String excerpt) {
		String text = excerpt.replaceAll("\\s+", " ").strip();
		String previous;
		do {
			previous = text;
			text = text.replaceAll("^(?:[\"'\u201C\u201D\u2018\u2019\u2026]|\\.{3})+", "")
				.replaceAll("(?:[\"'\u201C\u201D\u2018\u2019\u2026]|\\.{3}|[.;,:])+$", "")
				.strip();
		}
		while (!text.equals(previous));
		return text;
	}

	/** True when the excerpt is at least two words and appears in the line. */
	static boolean in(String excerpt, String line) {
		if (excerpt.length() < MIN_LENGTH || excerpt.strip().split("\\s+").length < 2) {
			return false;
		}
		return normalize(line).contains(normalize(excerpt));
	}

}
