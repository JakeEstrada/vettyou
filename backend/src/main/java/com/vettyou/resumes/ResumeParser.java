package com.vettyou.resumes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Splits a resume into contact details, jobs, projects, and schools by copying lines under headings. The file is
 * untrusted. Nothing in it is obeyed, and a missing month is left blank rather than filled in.
 */
@Component
public class ResumeParser {

	private static final int MAX_ENTRIES = 40;

	private static final String MONTH = "(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)";

	private static final String TOKEN = "(?:" + MONTH + "\\.?\\s+\\d{4}|\\d{1,2}/\\d{4}|\\d{4})";

	private static final String DATES = TOKEN + "(?:\\s*(?:-|–|—|to)\\s*(?:present|current|now|" + TOKEN + "))?";

	private static final Pattern RANGE = Pattern
		.compile("(?i)(" + TOKEN + ")\\s*(?:-|–|—|to)\\s*(present|current|now|" + TOKEN + ")");

	private static final Pattern TRAILING_DATES = Pattern
		.compile("(?i)^(.*?)[\\s,·|•–—-]*(?<![\\p{L}\\p{N}])(" + DATES + ")\\s*$");

	private static final Pattern MONTH_YEAR = Pattern.compile("(?i)(" + MONTH + ")\\.?\\s+(\\d{4})");

	private static final Pattern NUMERIC_MONTH = Pattern.compile("(\\d{1,2})/(\\d{4})");

	private static final Pattern PHONE = Pattern
		.compile("(?<!\\d)(?:\\+\\d{1,3}[\\s.-]?)?(?:\\(\\d{3}\\)[\\s.-]?|\\d{3}[\\s.-])\\d{3}[\\s.-]\\d{4}(?!\\d)");

	private static final Pattern EMAIL = Pattern
		.compile("(?<![\\w.+-])[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}(?![\\w-])");

	private static final Pattern URL = Pattern.compile("(?i)\\b(?:https?://|www\\.)[^\\s|•·<>]+");

	private static final Pattern DOMAIN = Pattern.compile(
			"^(?:[a-z0-9-]+\\.)+(?:com|org|net|dev|io|tech|app|co|me|ai|us|edu|info|site|xyz|page|so|sh|gg|tv|ca|uk)(?:/\\S*)?$");

	private static final Pattern LINKEDIN = Pattern
		.compile("(?i)(?:(?:https?://)?(?:www\\.)?linkedin\\.com/|(?<![\\w/])(?=in/))in/[A-Za-z0-9_%-]+/?");

	private static final Pattern CITY_STATE = Pattern
		.compile("(?<![\\p{L}])(?:Remote|[A-Z][A-Za-z.'-]*(?:[ \\t]+[A-Z][A-Za-z.'-]*){0,3},[ \\t]*[A-Z]{2})(?![A-Za-z])");

	private static final Pattern LOCATION = Pattern
		.compile("^(?:Remote|[A-Za-z][A-Za-z.'-]*(?:\\s+[A-Za-z][A-Za-z.'-]*){0,3},\\s*[A-Z]{2})$");

	private static final Pattern BULLET = Pattern.compile("^(?:[-*+]|•|●|▪|◦|\\d+[.)])\\s+(.*)$");

	private static final Pattern PARTS = Pattern.compile("\\s*(?:\\||•|·|—)\\s*");

	private static final Pattern AT = Pattern.compile("(?i)^(.+?)\\s+(?:at|@)\\s+(.+)$");

	private static final Pattern IN_FIELD = Pattern.compile("(?i)^(.+?)\\s+in\\s+(.+)$");

	private static final Pattern DEGREE_ABBREVIATION = Pattern.compile("^((?:[A-Za-z]{1,2}\\.){1,3}[A-Za-z]?)\\s+(.+)$");

	private static final Pattern DEGREE = Pattern.compile(
			"(?i)\\b(bachelor|master|doctorate|doctoral|ph\\.?d|b\\.?\\s?s\\.?|b\\.?\\s?a\\.?|m\\.?\\s?s\\.?|m\\.?\\s?a\\.?|a\\.?\\s?a\\.?|a\\.?\\s?s\\.?|associate|diploma|certificate|ged|high school)\\b");

	private static final Pattern TITLE_HINT = Pattern.compile(
			"(?i)\\b(engineer|developer|manager|director|intern|analyst|designer|lead|consultant|specialist|coordinator|assistant|officer|architect|scientist|administrator|technician|founder|owner|president|barista|cashier|tutor|teacher|nurse|accountant|associate|representative|supervisor|principal|partner|researcher|writer|editor|recruiter|producer|operator|mechanic|clerk|attendant|chef|server|bartender|driver|counselor|therapist|pharmacist|attorney|lawyer|paralegal|auditor|planner|strategist|marketer|executive|vp|ceo|cto|cfo)\\b");

	private static final Pattern ORG_HINT = Pattern
		.compile("(?i)\\b(inc|llc|ltd|corp|corporation|labs|laboratory|university|college|company|group|studio|gmbh|plc)\\b");

	private static final Pattern SENTENCE_BREAK = Pattern.compile("\\p{Ll}{2,}\\.\\s+\\p{Lu}");

	private static final Pattern FENCE = Pattern.compile("^\\s*(```|~~~)");

	private static final Set<String> CARRY_WORDS = Set.of("a", "an", "and", "are", "as", "at", "be", "but", "by", "for",
			"from", "in", "into", "is", "its", "my", "of", "on", "or", "our", "so", "than", "that", "the", "their", "to",
			"was", "were", "with", "&");

	private static final Set<String> DATE_LEADS = Set.of("about", "after", "also", "and", "around", "at", "before",
			"between", "by", "circa", "during", "from", "in", "of", "on", "or", "say", "said", "says", "since", "the",
			"through", "to", "until");

	private static final Map<String, String> HEADINGS = new LinkedHashMap<>();

	static {
		for (String heading : List.of("experience", "work experience", "professional experience", "employment",
				"work history", "relevant experience")) {
			HEADINGS.put(heading, "experience");
		}
		for (String heading : List.of("projects", "personal projects", "selected projects", "side projects")) {
			HEADINGS.put(heading, "projects");
		}
		for (String heading : List.of("education", "education and training", "academic background")) {
			HEADINGS.put(heading, "education");
		}
		for (String heading : List.of("summary", "professional summary", "objective", "profile", "about", "skills",
				"technical skills", "core competencies", "tools", "certifications", "certificates", "awards", "honors",
				"publications", "references", "interests", "activities", "volunteer", "volunteer experience",
				"volunteering", "leadership", "additional", "contact")) {
			HEADINGS.put(heading, "skip");
		}
	}

	private static final Map<String, Integer> MONTHS = Map.ofEntries(Map.entry("jan", 1), Map.entry("feb", 2),
			Map.entry("mar", 3), Map.entry("apr", 4), Map.entry("may", 5), Map.entry("jun", 6), Map.entry("jul", 7),
			Map.entry("aug", 8), Map.entry("sep", 9), Map.entry("oct", 10), Map.entry("nov", 11), Map.entry("dec", 12));

	public Parsed parse(String text) {
		List<String> header = new ArrayList<>();
		Map<String, List<String>> sections = new LinkedHashMap<>();
		String section = "header";
		boolean fenced = false;
		for (String raw : text.split("\\R", -1)) {
			String line = raw.strip();
			if (FENCE.matcher(line).find()) {
				fenced = !fenced;
				continue;
			}
			if (fenced) {
				continue;
			}
			String kind = headingKind(line);
			if (kind != null) {
				section = kind;
				sections.computeIfAbsent(section, key -> new ArrayList<>());
				continue;
			}
			if (line.isEmpty()) {
				continue;
			}
			if ("header".equals(section)) {
				header.add(line);
			} else if (!"skip".equals(section)) {
				sections.computeIfAbsent(section, key -> new ArrayList<>()).add(line);
			}
		}
		return new Parsed(contact(header), jobs(entries(sections.getOrDefault("experience", List.of()))),
				projects(entries(sections.getOrDefault("projects", List.of()))),
				schools(entries(sections.getOrDefault("education", List.of()))));
	}

	private static Contact contact(List<String> header) {
		String joined = String.join("\n", header);
		String email = group(EMAIL, joined);
		String phone = group(PHONE, joined);
		String linkedin = "";
		Matcher linkedIn = LINKEDIN.matcher(joined);
		if (linkedIn.find()) {
			String found = trimPunctuation(linkedIn.group());
			linkedin = found.startsWith("in/") ? "linkedin.com/" + found : found;
		}
		String website = "";
		for (String token : joined.split("[\\s|•·]+")) {
			String candidate = trimPunctuation(token);
			String lower = candidate.toLowerCase(Locale.ROOT);
			if (candidate.isEmpty() || candidate.indexOf('@') >= 0 || lower.contains("linkedin.com")
					|| lower.startsWith("in/")) {
				continue;
			}
			if (URL.matcher(candidate).matches() || DOMAIN.matcher(candidate).matches()) {
				website = candidate;
				break;
			}
		}
		String location = group(CITY_STATE, joined);
		String name = "";
		for (String line : header) {
			if (looksLikeName(line)) {
				name = line;
				break;
			}
		}
		return new Contact(clip(displayCase(name), 120), email.length() <= 200 ? email : "", clip(phone, 40),
				clip(location, 120), clip(website, 200), clip(linkedin, 200));
	}

	private static boolean looksLikeName(String line) {
		if (line.length() > 80 || line.indexOf('@') >= 0 || PHONE.matcher(line).find() || URL.matcher(line).find()
				|| LINKEDIN.matcher(line).find() || CITY_STATE.matcher(line).find() || headingKind(line) != null) {
			return false;
		}
		String[] words = line.split("\\s+");
		if (words.length > 6) {
			return false;
		}
		for (String word : words) {
			if (word.chars().noneMatch(Character::isLetter)) {
				return false;
			}
		}
		return true;
	}

	private static String displayCase(String name) {
		if (name.isBlank() || !name.equals(name.toUpperCase(Locale.ROOT))) {
			return name;
		}
		StringBuilder out = new StringBuilder();
		boolean start = true;
		for (char character : name.toLowerCase(Locale.ROOT).toCharArray()) {
			out.append(start ? Character.toUpperCase(character) : character);
			start = character == ' ' || character == '-' || character == '\'';
		}
		return out.toString();
	}

	/**
	 * Groups lines into entries. A line that ends in a date starts an entry. In a section that uses bullet marks, a
	 * plain line after bullets also starts one, which covers resumes that put the title, company, and dates on separate
	 * lines.
	 */
	private static List<Entry> entries(List<String> lines) {
		List<String> joined = joinWrapped(lines);
		boolean marked = joined.stream().anyMatch(line -> BULLET.matcher(line).matches());
		List<Entry> entries = new ArrayList<>();
		Entry current = null;
		boolean sawBullet = false;
		for (String line : joined) {
			boolean bullet = BULLET.matcher(line).matches();
			Dated dated = bullet ? null : dated(line);
			if (dated != null && !dated.text().isEmpty()) {
				current = new Entry(dated.text(), dated.dates());
				entries.add(current);
				sawBullet = false;
				continue;
			}
			if (dated != null && current != null && current.dates == null) {
				current.dates = dated.dates();
				continue;
			}
			if (current == null || (marked && !bullet && sawBullet)) {
				current = new Entry(bullet ? stripBullet(line) : line, null);
				entries.add(current);
				sawBullet = false;
				continue;
			}
			current.lines.add(line);
			sawBullet |= bullet;
		}
		return entries.size() > MAX_ENTRIES ? entries.subList(0, MAX_ENTRIES) : entries;
	}

	/** PDFs wrap long sentences onto the next line. Put them back together before splitting entries. */
	private static List<String> joinWrapped(List<String> lines) {
		int widest = lines.stream().mapToInt(String::length).max().orElse(0);
		int full = (int) Math.ceil(widest * 0.85);
		List<String> out = new ArrayList<>();
		String previousRaw = null;
		for (String line : lines) {
			boolean join = previousRaw != null && continues(previousRaw, line, previousRaw.length() >= full);
			previousRaw = line;
			if (join) {
				out.set(out.size() - 1, out.get(out.size() - 1) + " " + line);
			} else {
				out.add(line);
			}
		}
		return out;
	}

	/**
	 * A line starting in lowercase always continues the one above. Otherwise the line above has to run close to the
	 * full width, which is how a PDF wrap looks, and end mid-phrase.
	 */
	private static boolean continues(String previous, String line, boolean previousFullWidth) {
		if (BULLET.matcher(line).matches() || dated(previous) != null || dated(line) != null) {
			return false;
		}
		if (Character.isLowerCase(line.codePointAt(0))) {
			return true;
		}
		if (!previousFullWidth) {
			return false;
		}
		String[] words = previous.split("\\s+");
		if (previous.endsWith(",") || CARRY_WORDS.contains(words[words.length - 1].toLowerCase(Locale.ROOT))) {
			return true;
		}
		String first = line.split("\\s+")[0];
		return !previous.matches(".*[.!?:;)]$") && first.length() > 1 && first.endsWith(".");
	}

	private static Dated dated(String line) {
		Matcher matcher = TRAILING_DATES.matcher(line);
		if (!matcher.matches()) {
			return null;
		}
		String text = matcher.group(1).replaceAll("[\\s,·|•–—-]+$", "").strip();
		if (!text.isEmpty()) {
			String[] words = text.split("\\s+");
			String last = words[words.length - 1].toLowerCase(Locale.ROOT);
			if (text.length() > 140 || text.endsWith(".") || DATE_LEADS.contains(last)
					|| SENTENCE_BREAK.matcher(text).find()) {
				return null;
			}
		}
		return new Dated(text, matcher.group(2).strip());
	}

	private List<Job> jobs(List<Entry> entries) {
		List<Job> jobs = new ArrayList<>();
		for (Entry entry : entries) {
			Job job = job(entry);
			if (job != null) {
				jobs.add(job);
			}
		}
		return jobs;
	}

	private Job job(Entry entry) {
		Shape shape = shape(entry);
		List<String> facts = new ArrayList<>();
		List<String> notes = new ArrayList<>();
		String location = "";
		for (String part : shape.facts()) {
			if (location.isEmpty() && LOCATION.matcher(part).matches()) {
				location = part;
			} else {
				facts.add(part);
			}
		}
		for (String part : shape.meta()) {
			if (location.isEmpty() && LOCATION.matcher(part).matches()) {
				location = part;
			} else {
				notes.add(part);
			}
		}
		String title = "";
		String employer = "";
		if (facts.size() == 1) {
			Matcher at = AT.matcher(facts.get(0));
			if (at.matches() && shortLabel(at.group(1)) && shortLabel(at.group(2))) {
				title = at.group(1).strip();
				employer = at.group(2).strip();
			} else {
				title = facts.get(0);
				employer = facts.get(0);
			}
		} else if (facts.size() >= 2) {
			String first = facts.get(0);
			String second = facts.get(1);
			boolean firstTitle = isTitle(first);
			boolean secondTitle = isTitle(second);
			boolean firstOrg = isOrg(first);
			boolean secondOrg = isOrg(second);
			if (secondTitle && !firstTitle || firstOrg && !secondOrg && !firstTitle) {
				title = second;
				employer = first;
			} else {
				title = first;
				employer = second;
			}
			notes.addAll(0, facts.subList(2, facts.size()));
		}
		title = clip(title, 160);
		employer = clip(employer, 160);
		if (title.isBlank() || employer.isBlank()) {
			return null;
		}
		When when = when(entry.dates);
		List<String> body = new ArrayList<>();
		if (!notes.isEmpty()) {
			body.add(String.join(" · ", notes));
		}
		body.addAll(shape.items());
		if (entry.dates != null && (when.start() == null || !when.current() && when.end() == null)) {
			body.add(entry.dates);
		}
		String text = String.join("\n", body).strip();
		if (text.isBlank()) {
			text = title;
		}
		if (text.length() > 20000) {
			text = text.substring(0, 20000);
		}
		boolean ranged = when.start() != null;
		return new Job(title, employer, clip(location, 120), ranged ? when.start() : null, ranged ? when.end() : null,
				when.current(), text);
	}

	private List<ProjectBlock> projects(List<Entry> entries) {
		List<ProjectBlock> projects = new ArrayList<>();
		for (Entry entry : entries) {
			ProjectBlock project = project(entry);
			if (project != null) {
				projects.add(project);
			}
		}
		return projects;
	}

	private ProjectBlock project(Entry entry) {
		Shape shape = shape(entry);
		String url = "";
		List<String> facts = new ArrayList<>();
		for (String part : shape.facts()) {
			if (isUrl(part)) {
				url = url.isBlank() ? trimPunctuation(part) : url;
			} else {
				facts.add(part);
			}
		}
		List<String> stack = new ArrayList<>();
		for (String part : shape.meta()) {
			if (isUrl(part)) {
				url = url.isBlank() ? trimPunctuation(part) : url;
			} else {
				stack.add(part);
			}
		}
		if (facts.isEmpty()) {
			return null;
		}
		String name = clip(facts.get(0), 200);
		if (name.isBlank()) {
			return null;
		}
		String summary = truncate(String.join(" · ", facts.subList(1, facts.size())), 4000);
		StringBuilder markdown = new StringBuilder("# ").append(name).append("\n\n");
		if (!summary.isBlank()) {
			markdown.append(summary).append("\n\n");
		}
		if (!stack.isEmpty()) {
			markdown.append(String.join(" · ", stack)).append("\n\n");
		}
		for (String item : shape.items()) {
			markdown.append("- ").append(item).append('\n');
		}
		return new ProjectBlock(name, summary, clip(url, 500), markdown.toString().strip() + "\n");
	}

	private List<School> schools(List<Entry> entries) {
		List<School> schools = new ArrayList<>();
		for (Entry entry : entries) {
			School school = school(entry);
			if (school != null) {
				schools.add(school);
			}
		}
		return schools;
	}

	private School school(Entry entry) {
		Shape shape = shape(entry);
		String degreeLine = null;
		String institution = null;
		for (String fact : shape.facts()) {
			if (LOCATION.matcher(fact).matches()) {
				continue;
			}
			if (degreeLine == null && isDegree(fact)) {
				degreeLine = fact;
			} else if (institution == null) {
				institution = fact;
			}
		}
		if (degreeLine == null) {
			for (String line : shape.items()) {
				if (isDegree(line) && line.length() <= 160) {
					degreeLine = line;
					break;
				}
			}
		}
		if (institution == null) {
			institution = degreeLine;
			degreeLine = null;
		}
		institution = clip(institution == null ? "" : institution, 160);
		if (institution.isBlank()) {
			return null;
		}
		String credential = "";
		String field = "";
		if (degreeLine != null) {
			Matcher inField = IN_FIELD.matcher(degreeLine);
			Matcher abbreviation = DEGREE_ABBREVIATION.matcher(degreeLine);
			if (inField.matches() && isDegree(inField.group(1))) {
				credential = inField.group(1).strip();
				field = inField.group(2).strip();
			} else if (abbreviation.matches()) {
				credential = abbreviation.group(1).strip();
				field = abbreviation.group(2).strip();
			} else {
				credential = degreeLine;
			}
		}
		When when = when(entry.dates);
		if (entry.dates != null && when.start() == null && when.end() == null) {
			field = field.isBlank() ? entry.dates : field + " (" + entry.dates + ")";
		}
		return new School(institution, truncate(credential, 160), truncate(field, 160), when.start(), when.end());
	}

	/**
	 * Separates an entry into the facts that name it, the short line under it (place, link, or stack), and the
	 * sentences that describe it.
	 */
	private static Shape shape(Entry entry) {
		List<String> facts = new ArrayList<>(splitParts(entry.header));
		List<String> meta = new ArrayList<>();
		List<String> items = new ArrayList<>();
		boolean marked = entry.lines.stream().anyMatch(line -> BULLET.matcher(line).matches());
		for (int index = 0; index < entry.lines.size(); index++) {
			String line = entry.lines.get(index);
			Matcher bullet = BULLET.matcher(line);
			if (bullet.matches()) {
				items.add(bullet.group(1).strip());
			} else if (marked) {
				facts.addAll(splitParts(line));
			} else if (index == 0 && isMeta(line)) {
				meta.addAll(splitParts(line));
			} else {
				items.add(line);
			}
		}
		return new Shape(facts, meta, items);
	}

	private static boolean isMeta(String line) {
		if (line.contains(" · ") || line.contains(" | ")) {
			return true;
		}
		if (line.endsWith(".") || !line.contains(",")) {
			return false;
		}
		for (String piece : line.split(",")) {
			if (piece.isBlank() || piece.strip().split("\\s+").length > 4) {
				return false;
			}
		}
		return true;
	}

	private static When when(String dates) {
		if (dates == null) {
			return new When(null, null, false);
		}
		Matcher range = RANGE.matcher(dates);
		if (!range.find()) {
			return new When(null, monthDate(dates), false);
		}
		boolean current = range.group(2).matches("(?i)present|current|now");
		LocalDate start = monthDate(range.group(1));
		LocalDate end = current ? null : monthDate(range.group(2));
		if (start != null && end != null && end.isBefore(start)) {
			return new When(null, null, false);
		}
		return new When(start, end, current);
	}

	private static LocalDate monthDate(String token) {
		Matcher month = MONTH_YEAR.matcher(token.strip());
		if (month.matches()) {
			Integer value = MONTHS.get(month.group(1).toLowerCase(Locale.ROOT).substring(0, 3));
			if (value != null) {
				return LocalDate.of(Integer.parseInt(month.group(2)), value, 1);
			}
		}
		Matcher numeric = NUMERIC_MONTH.matcher(token.strip());
		if (numeric.matches()) {
			int value = Integer.parseInt(numeric.group(1));
			if (value >= 1 && value <= 12) {
				return LocalDate.of(Integer.parseInt(numeric.group(2)), value, 1);
			}
		}
		return null;
	}

	private static String headingKind(String line) {
		String stripped = line.replaceFirst("^#{1,6}\\s+", "").replaceFirst("[:\\s]+$", "").strip();
		if (stripped.length() > 40) {
			return null;
		}
		return HEADINGS.get(stripped.toLowerCase(Locale.ROOT));
	}

	private static List<String> splitParts(String line) {
		List<String> parts = new ArrayList<>();
		for (String piece : PARTS.split(line)) {
			String trimmed = piece.strip();
			if (!trimmed.isEmpty()) {
				parts.add(trimmed);
			}
		}
		return parts;
	}

	private static String stripBullet(String line) {
		Matcher bullet = BULLET.matcher(line);
		return bullet.matches() ? bullet.group(1).strip() : line;
	}

	private static boolean shortLabel(String value) {
		String trimmed = value.strip();
		if (trimmed.isEmpty() || trimmed.length() > 80 || !Character.isUpperCase(trimmed.charAt(0))) {
			return false;
		}
		return trimmed.split("\\s+").length <= 6;
	}

	private static boolean isTitle(String value) {
		return TITLE_HINT.matcher(value).find();
	}

	private static boolean isOrg(String value) {
		return ORG_HINT.matcher(value).find();
	}

	private static boolean isDegree(String value) {
		return DEGREE.matcher(value).find();
	}

	private static boolean isUrl(String value) {
		String candidate = trimPunctuation(value);
		return URL.matcher(candidate).matches() || DOMAIN.matcher(candidate).matches()
				|| candidate.toLowerCase(Locale.ROOT).startsWith("github.com/");
	}

	private static String group(Pattern pattern, String text) {
		Matcher matcher = pattern.matcher(text);
		return matcher.find() ? matcher.group().strip() : "";
	}

	private static String trimPunctuation(String value) {
		return value.replaceAll("[.,);\\]>]+$", "");
	}

	private static String clip(String value, int max) {
		if (value == null) {
			return "";
		}
		String trimmed = value.strip();
		return trimmed.length() <= max ? trimmed : "";
	}

	private static String truncate(String value, int max) {
		if (value == null) {
			return "";
		}
		String trimmed = value.strip();
		return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
	}

	private static final class Entry {

		private final String header;

		private String dates;

		private final List<String> lines = new ArrayList<>();

		private Entry(String header, String dates) {
			this.header = header;
			this.dates = dates;
		}

	}

	private record Dated(String text, String dates) {
	}

	private record Shape(List<String> facts, List<String> meta, List<String> items) {
	}

	private record When(LocalDate start, LocalDate end, boolean current) {
	}

	public record Parsed(Contact contact, List<Job> jobs, List<ProjectBlock> projects, List<School> schools) {
	}

	public record Contact(String name, String email, String phone, String location, String website, String linkedin) {
	}

	public record Job(String title, String employer, String location, LocalDate start, LocalDate end, boolean current,
			String body) {
	}

	public record ProjectBlock(String name, String summary, String url, String markdown) {
	}

	public record School(String institution, String credential, String field, LocalDate start, LocalDate end) {
	}

}
