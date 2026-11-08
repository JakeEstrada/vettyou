package com.vettyou.matching;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * One of the person's own lines, ranked for a posting. {@code key} identifies it whatever its number. {@code gain} is
 * the share of the posting it covers that lines ranked before it do not, 0 when it adds nothing new. {@code quality}
 * rates its wording from 0.4 to 1. {@code whole} is false when the line is too long to print.
 */
public record RankedLine(String key, String kind, UUID sourceId, String label, String text, boolean confirmed,
		boolean whole, double gain, double quality, List<LineBacking> backs) {

	private static final Pattern SEPARATOR = Pattern.compile("\\s[\u00b7|]\\s");

	private static final Pattern OTHER_RESUME = Pattern
		.compile("(?i)\\b(?:older|one|some|other|earlier|previous|past)\\s+(?:older\\s+)?resumes?\\s+(?:also\\s+)?"
				+ "(?:says?|said|describes?|calls?|adds?|lists?|names?|mentions?)\\b");

	/**
	 * A note to oneself, not a bullet: "Own practice · example.com · Lead Developer", or a line that reports what
	 * another resume says. Lines are never reworded, so a line holding a note is never printed.
	 */
	public static boolean noteLike(String text) {
		return SEPARATOR.matcher(text).results().count() >= 2 || OTHER_RESUME.matcher(text).find();
	}

}
