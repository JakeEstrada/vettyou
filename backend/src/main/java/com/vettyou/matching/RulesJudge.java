package com.vettyou.matching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.vettyou.evidence.StackDetector;
import com.vettyou.profile.EducationResponse;

/**
 * The deterministic matcher, used when no AI key is set and for postings read before parts existed. A part counts only
 * when a line names the listed technology or contains the part's own words in order; degrees compare level and field.
 */
final class RulesJudge {

	static final String NOT_FOUND = "Not found in your job write-ups, project lines, or education.";

	private static final int CITES = 3;

	private static final int EXCERPT_LIMIT = 240;

	private static final Pattern YEARS = Pattern.compile("(?i)\\b\\d+\\s*\\+?\\s*(?:-\\s*\\d+\\s*)?(?:years?|yrs?)\\b");

	private static final Pattern EQUIVALENT = Pattern.compile("(?i)\\bequivalent\\b|\\bin lieu\\b");

	private static final Pattern RELATED = Pattern.compile("(?i)\\brelated\\b|\\bany field\\b|\\bsimilar\\b|\\bequivalent\\b");

	private static final Set<String> STOP = Set.of("and", "or", "the", "of", "in", "with", "for", "to", "a", "an",
			"on", "at", "by", "as", "is", "be", "are", "using", "use", "experience", "experienced", "knowledge",
			"familiarity", "familiar", "proficiency", "proficient", "understanding", "ability", "strong", "solid",
			"working", "hands", "skills", "skill", "plus", "years", "year", "degree", "related", "field",
			"equivalent", "etc", "professional", "good", "excellent", "demonstrated", "proven");

	private final StackDetector detector;

	RulesJudge(StackDetector detector) {
		this.detector = detector;
	}

	PartJudgment judge(RequirementInput requirement, PartInput part, EvidenceIndex index, Library library) {
		if ("education".equals(requirement.category())) {
			return education(requirement, index, library);
		}
		Pattern pattern = techPattern(part.text()).orElseGet(() -> phrase(part.text()));
		List<EvidenceRef> found = pattern == null ? List.of() : find(index, pattern);
		boolean years = YEARS.matcher(part.text()).find()
				|| requirement.parts().size() == 1 && YEARS.matcher(requirement.skill()).find();
		if (years) {
			return new PartJudgment("unclear", found.isEmpty()
					? "Years of experience are not counted automatically. Check your job dates."
					: "Related work is shown. Years are not counted automatically; check your job dates.", found);
		}
		if (!found.isEmpty()) {
			return new PartJudgment("direct", "Found in your own lines.", found);
		}
		if ("credential".equals(requirement.category())) {
			return new PartJudgment("none", "Only counts if you hold it. Add it to a job or your education if you do.",
					List.of());
		}
		return new PartJudgment("none", NOT_FOUND, List.of());
	}

	/** Every line the pattern finds, one per piece of work, confirmed lines first. */
	static List<EvidenceRef> find(EvidenceIndex index, Pattern pattern) {
		List<EvidenceRef> found = new ArrayList<>();
		Set<String> groups = new HashSet<>();
		index.lines()
			.stream()
			.filter(line -> pattern.matcher(line.text()).find())
			.sorted(Comparator.comparing(line -> !line.confirmed()))
			.filter(line -> groups.add(line.group()))
			.limit(CITES)
			.forEach(line -> found.add(new EvidenceRef(line.kind(), line.sourceId(), line.text(), excerpt(line.text()))));
		return found;
	}

	/** The listed technology's pattern when the text is just that name, e.g. "Postgres". */
	Optional<Pattern> techPattern(String text) {
		String canonical = detector.canonical(text.strip());
		return detector.pattern(canonical).filter(pattern -> pattern.matcher(text.strip()).matches());
	}

	private PartJudgment education(RequirementInput requirement, EvidenceIndex index, Library library) {
		String text = (requirement.skill() + " " + requirement.quote()).toLowerCase(Locale.ROOT);
		int needed = neededLevel(text);
		EducationResponse best = null;
		boolean bestField = false;
		for (EducationResponse school : library.schools()) {
			if (credentialLevel(school.credential()) < needed) {
				continue;
			}
			boolean field = fieldMatches(school.fieldOfStudy(), text);
			if (best == null || field && !bestField) {
				best = school;
				bestField = field;
			}
		}
		if (best == null) {
			return EQUIVALENT.matcher(text).find()
					? new PartJudgment("unclear", "No degree at this level, but the posting accepts equivalent experience.",
							List.of())
					: new PartJudgment("none", "No degree at this level in your education.", List.of());
		}
		String degree = (best.credential() + " " + best.fieldOfStudy()).strip();
		EducationResponse school = best;
		List<EvidenceRef> evidence = index.lines()
			.stream()
			.filter(line -> "school".equals(line.kind()) && line.sourceId().equals(school.id()))
			.limit(1)
			.map(line -> new EvidenceRef("school", school.id(), line.text(), excerpt(line.text())))
			.toList();
		if (bestField) {
			return new PartJudgment("direct", degree + " matches.", evidence);
		}
		if (RELATED.matcher(text).find() || !text.contains(" in ")) {
			return new PartJudgment("direct", degree + " meets the level.", evidence);
		}
		return new PartJudgment("transferable", degree + " meets the level; the field may not match.", evidence);
	}

	private static int neededLevel(String text) {
		int needed = Integer.MAX_VALUE;
		if (text.matches("(?s).*\\b(ph\\.?d|doctorate|doctoral)(?![a-z]).*")) {
			needed = Math.min(needed, 5);
		}
		if (text.matches("(?s).*\\b(master'?s?|m\\.s\\.|msc|mba)(?![a-z]).*")) {
			needed = Math.min(needed, 4);
		}
		if (text.matches("(?s).*\\b(bachelor'?s?|b\\.s\\.?|b\\.a\\.?|bs|ba|bsc|undergraduate|four-year|4-year)(?![a-z]).*")) {
			needed = Math.min(needed, 3);
		}
		if (text.matches("(?s).*\\b(associate'?s?|a\\.a\\.?|a\\.s\\.?)(?![a-z]).*")) {
			needed = Math.min(needed, 2);
		}
		if (text.matches("(?s).*\\b(high school|ged|diploma)(?![a-z]).*")) {
			needed = Math.min(needed, 1);
		}
		if (needed == Integer.MAX_VALUE) {
			needed = text.contains("degree") ? 3 : 1;
		}
		return needed;
	}

	private static int credentialLevel(String credential) {
		String compact = credential.toLowerCase(Locale.ROOT).replaceAll("[.\\s']", "");
		if (compact.matches("(phd|doctor|dphil).*")) {
			return 5;
		}
		if (compact.matches("(ms|ma|msc|mba|meng|master).*")) {
			return 4;
		}
		if (compact.matches("(bs|ba|bsc|bba|beng|bachelor).*")) {
			return 3;
		}
		if (compact.matches("(aa|as|aas|associate).*")) {
			return 2;
		}
		if (compact.contains("diploma") || compact.contains("ged") || compact.contains("highschool")) {
			return 1;
		}
		return 0;
	}

	private static boolean fieldMatches(String field, String text) {
		return Arrays.stream(field.toLowerCase(Locale.ROOT).split("[^a-z]+"))
			.filter(word -> word.length() >= 4 && !STOP.contains(word))
			.anyMatch(text::contains);
	}

	/**
	 * Matches the part's own words in order, allowing word endings and one filler word between them. "A or B" and
	 * "A, B" match either side.
	 */
	static Pattern phrase(String skill) {
		List<String> alternatives = Arrays.stream(skill.split("(?i)\\s+(?:and/)?or\\s+|\\s*,\\s*|\\s+/\\s+"))
			.map(RulesJudge::words)
			.filter(words -> !words.isEmpty())
			.map(words -> words.stream()
				.map(RulesJudge::stem)
				.collect(Collectors.joining("[\\s\\-/]+(?:[a-z]+[\\s\\-/]+)?")))
			.toList();
		if (alternatives.isEmpty()) {
			return null;
		}
		return Pattern.compile("(?<![a-z0-9])(?:" + String.join("|", alternatives) + ")", Pattern.CASE_INSENSITIVE);
	}

	private static List<String> words(String text) {
		return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9+#.]+"))
			.map(word -> word.replaceAll("^\\.+|\\.+$", ""))
			.filter(word -> word.length() >= 2 && !STOP.contains(word) && !word.matches("[0-9+]+"))
			.toList();
	}

	private static String stem(String word) {
		String noun = word.replaceFirst("(?:ations?|ions?)$", "");
		String root = noun.length() >= 5 && !noun.equals(word) ? noun
				: word.length() > 5 ? word.replaceFirst("(?:ing|ed|es|s)$", "") : word;
		return Pattern.quote(root) + (root.length() >= 4 ? "[a-z]*" : "(?![a-z0-9])");
	}

	private static String excerpt(String text) {
		if (text.length() <= EXCERPT_LIMIT) {
			return text;
		}
		int cut = text.lastIndexOf(' ', EXCERPT_LIMIT);
		return text.substring(0, cut > 0 ? cut : EXCERPT_LIMIT).strip();
	}

}
