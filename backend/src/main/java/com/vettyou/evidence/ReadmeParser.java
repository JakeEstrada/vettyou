package com.vettyou.evidence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Reads the outline of a README: its title, first paragraph, and the bullet points that describe what was built. The
 * README is untrusted text. Nothing here interprets it; every value is copied from the file.
 */
@Component
public class ReadmeParser {

	public record Feature(String name, String section, int lineNumber, String quote, boolean planned) {
	}

	private static final int NAME_LIMIT = 1000;

	private static final int FEATURE_LIMIT = 1000;

	private static final Pattern ATX_HEADING = Pattern.compile("^\\s{0,3}(#{1,6})\\s+(.*?)\\s*#*\\s*$");

	private static final Pattern SETEXT_UNDERLINE = Pattern.compile("^\\s{0,3}(=+|-+)\\s*$");

	private static final Pattern FENCE = Pattern.compile("^\\s*(```|~~~)");

	private static final Pattern BULLET = Pattern
		.compile("^\\s*(?:[-*+]|\\d+[.)])\\s+(?:\\[([ xX])\\]\\s+)?(.+)$");

	private static final Pattern PLANNED_SECTION = Pattern
		.compile("(?i)\\b(roadmap|todo|to-do|future|planned|coming soon|next steps|wishlist|ideas|not yet)\\b");

	private static final Pattern ANCHOR_ONLY = Pattern.compile("\\[[^\\]]*\\]\\(#[^)]*\\)");

	private static final Pattern LINK = Pattern.compile("!?\\[([^\\]]*)\\]\\([^)]*\\)");

	public String title(String body, String fileName) {
		return heading(body).orElseGet(() -> {
			String base = fileName.replaceFirst("(?i)\\.(md|markdown|txt)$", "").replaceAll("[-_]+", " ").trim();
			return base.isEmpty() || base.equalsIgnoreCase("readme") ? "Untitled project" : limit(base, 160);
		});
	}

	public Optional<String> heading(String body) {
		String[] lines = body.split("\\R", -1);
		for (int index = 0; index < lines.length; index++) {
			if (FENCE.matcher(lines[index]).find()) {
				break;
			}
			Matcher heading = ATX_HEADING.matcher(lines[index]);
			if (heading.matches() && heading.group(1).length() == 1) {
				String text = clean(heading.group(2));
				if (!text.isEmpty()) {
					return Optional.of(limit(text, 160));
				}
			}
			if (index + 1 < lines.length && !lines[index].isBlank()
					&& lines[index + 1].trim().startsWith("=") && SETEXT_UNDERLINE.matcher(lines[index + 1]).matches()) {
				return Optional.of(limit(clean(lines[index]), 160));
			}
		}
		return Optional.empty();
	}

	public String summary(String body) {
		String[] lines = body.split("\\R", -1);
		List<String> paragraph = new ArrayList<>();
		boolean inFence = false;
		for (String line : lines) {
			if (FENCE.matcher(line).find()) {
				inFence = !inFence;
				if (!paragraph.isEmpty()) {
					break;
				}
				continue;
			}
			if (inFence) {
				continue;
			}
			String trimmed = line.trim();
			boolean prose = !trimmed.isEmpty() && !ATX_HEADING.matcher(line).matches()
					&& !SETEXT_UNDERLINE.matcher(line).matches() && !BULLET.matcher(line).matches()
					&& !trimmed.startsWith("<") && !trimmed.startsWith("|") && !trimmed.startsWith(">")
					&& !trimmed.startsWith("![") && !trimmed.startsWith("[![");
			if (prose) {
				paragraph.add(trimmed);
			}
			else if (!paragraph.isEmpty()) {
				break;
			}
		}
		return limit(clean(String.join(" ", paragraph)), 4000);
	}

	/**
	 * Every bullet point in the README, in order, outside code blocks. Bullets under a roadmap/todo heading, and
	 * unchecked {@code [ ]} items, are marked planned.
	 */
	public List<Feature> features(String body) {
		Map<String, Feature> found = new LinkedHashMap<>();
		String[] lines = body.split("\\R", -1);
		String[] headings = new String[7];
		boolean inFence = false;
		for (int index = 0; index < lines.length && found.size() < FEATURE_LIMIT; index++) {
			String line = lines[index];
			if (FENCE.matcher(line).find()) {
				inFence = !inFence;
				continue;
			}
			if (inFence) {
				continue;
			}
			Matcher heading = ATX_HEADING.matcher(line);
			if (heading.matches()) {
				int level = heading.group(1).length();
				headings[level] = clean(heading.group(2));
				for (int deeper = level + 1; deeper < headings.length; deeper++) {
					headings[deeper] = null;
				}
				continue;
			}
			Matcher bullet = BULLET.matcher(line);
			if (!bullet.matches() || ANCHOR_ONLY.matcher(bullet.group(2).trim()).matches()) {
				continue;
			}
			String name = limit(clean(bullet.group(2)), NAME_LIMIT);
			if (!name.matches(".*[\\p{L}\\p{N}].*") || found.containsKey(name.toLowerCase(Locale.ROOT))) {
				continue;
			}
			boolean planned = " ".equals(bullet.group(1));
			String section = "README";
			for (int level = 1; level < headings.length; level++) {
				if (headings[level] == null) {
					continue;
				}
				planned = planned || PLANNED_SECTION.matcher(headings[level]).find();
				if (level > 1 || section.equals("README")) {
					section = headings[level];
				}
			}
			found.put(name.toLowerCase(Locale.ROOT),
					new Feature(name, limit(section, 120), index + 1, limit(line.strip(), NAME_LIMIT), planned));
		}
		return new ArrayList<>(found.values());
	}

	private static String clean(String text) {
		String plain = LINK.matcher(text).replaceAll("$1");
		plain = plain.replace("**", "").replace("__", "").replace("`", "");
		return plain.replaceAll("\\s+", " ").trim();
	}

	private static String limit(String text, int max) {
		return text.length() <= max ? text : text.substring(0, max).trim();
	}

}
