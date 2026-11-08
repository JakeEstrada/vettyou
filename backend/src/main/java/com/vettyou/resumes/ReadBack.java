package com.vettyou.resumes;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.vettyou.profile.ProfileResponse;
import com.vettyou.templates.AtsFirstTemplate;
import com.vettyou.templates.ResumeDocument;

/**
 * Checks a resume by reading its PDF back as plain text. Spacing is ignored, since parsers differ on it; everything
 * else must come back as printed and in the printed order.
 */
final class ReadBack {

	static final Set<String> STANDARD_HEADINGS = Set.of("SKILLS", "EXPERIENCE", "PROJECTS", "EDUCATION");

	private ReadBack() {
	}

	static List<ReadCheck> checks(ResumeDocument document, ProfileResponse profile, AtsFirstTemplate.Rendered rendered,
			String text) {
		String squashed = squash(text);
		List<String> lines = text.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
		List<ReadCheck> checks = new ArrayList<>();

		checks.add(check("text", "Text can be read", !squashed.isEmpty(),
				"No text came out of the PDF. Build the resume again."));

		boolean named = !document.name().isBlank();
		boolean nameFirst = named && !lines.isEmpty() && !rendered.fragments().isEmpty()
				&& squash(lines.get(0)).equals(squash(rendered.fragments().get(0)));
		checks.add(check("name", "Your name comes first", nameFirst,
				named ? "Your name did not read back as the first line." : "Add your name on the profile page."));

		checks.add(contact("email", "Email is on it", profile.email(), squashed,
				"Add the email you apply with on the profile page."));
		checks.add(contact("phone", "Phone is on it", profile.phone(), squashed,
				"Add a phone number on the profile page."));

		String heading = headings(document, lines);
		checks.add(check("headings", "Standard section headings, in order", heading == null,
				"This heading did not read back in place: " + heading + "."));

		String missing = missing(rendered.fragments(), squashed);
		checks.add(check("order", "Every line reads back in order", missing == null,
				"This text did not read back where it is shown: \"" + missing + "\"."));

		checks.add(check("pages", "Fits on one page", rendered.pages() == 1,
				"It runs to " + rendered.pages() + " pages, because your jobs and schools alone do not fit on one."));

		checks.add(check("characters", "Every character prints", rendered.unprintable().isEmpty(),
				"These characters print as \"?\": " + String.join(" ", rendered.unprintable())
						+ ". Replace them in the lines that use them."));
		return List.copyOf(checks);
	}

	/** Removes all spacing and folds look-alike characters, so only the characters themselves are compared. */
	static String squash(String text) {
		return Normalizer.normalize(text, Normalizer.Form.NFKC).replaceAll("[\\s\\u00A0]+", "");
	}

	private static ReadCheck check(String key, String label, boolean passed, String failure) {
		return new ReadCheck(key, label, passed, passed ? "" : failure);
	}

	private static ReadCheck contact(String key, String label, String value, String squashed, String missing) {
		if (value == null || value.isBlank()) {
			return new ReadCheck(key, label, false, missing);
		}
		String wanted = squash(value);
		return check(key, label, squashed.contains(wanted), "It did not read back as \"" + value.strip() + "\".");
	}

	/** The first heading that is not standard or does not read back as its own line in order, or null. */
	private static String headings(ResumeDocument document, List<String> lines) {
		int at = 0;
		for (ResumeDocument.Section section : document.sections()) {
			String heading = section.heading().toUpperCase(Locale.ROOT);
			if (!STANDARD_HEADINGS.contains(heading)) {
				return heading;
			}
			int found = -1;
			for (int i = at; i < lines.size(); i++) {
				if (lines.get(i).toUpperCase(Locale.ROOT).equals(heading)) {
					found = i;
					break;
				}
			}
			if (found < 0) {
				return heading;
			}
			at = found + 1;
		}
		return null;
	}

	/** The first printed piece that does not read back after the ones before it, or null. */
	private static String missing(List<String> fragments, String squashed) {
		int at = 0;
		for (String fragment : fragments) {
			String piece = squash(fragment);
			if (piece.isEmpty()) {
				continue;
			}
			int found = squashed.indexOf(piece, at);
			if (found < 0) {
				return fragment;
			}
			at = found + piece.length();
		}
		return null;
	}

}
