package com.vettyou.matching;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.vettyou.evidence.ClaimView;
import com.vettyou.profile.EducationResponse;
import com.vettyou.profile.ExperienceText;

/**
 * The person's own lines, numbered so a match can cite them: job titles and write-up lines, project lines that are not
 * planned or marked not mine, and schools. Rebuilt from saved data on every use.
 */
final class EvidenceIndex {

	private static final int LINE_LIMIT = 400;

	private static final int MAX_LINES = 400;

	private static final Pattern BULLET = Pattern.compile("^\\s*(?:[-*+•●▪◦]|\\d+[.)])\\s+");

	/** The source id of skills-section lines, which belong to no job, project, or school. */
	static final UUID SKILLS = new UUID(0, 0);

	private final List<EvidenceLine> lines;

	private final Map<String, EvidenceLine> byRef = new HashMap<>();

	private final Map<String, List<EvidenceLine>> bySource = new HashMap<>();

	private EvidenceIndex(List<EvidenceLine> lines) {
		this.lines = List.copyOf(lines);
		for (EvidenceLine line : lines) {
			byRef.put(line.ref(), line);
			bySource.computeIfAbsent(source(line.kind(), line.sourceId()), key -> new ArrayList<>()).add(line);
		}
	}

	static EvidenceIndex of(Library library) {
		List<EvidenceLine> lines = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		Map<UUID, Pattern> projectNames = projectPatterns(library);
		for (ExperienceText job : library.jobs()) {
			String label = job.dates().isEmpty() ? job.label() : job.label() + ", " + job.dates();
			add(lines, seen, "job", job.id(), label, clean(job.title()), true, "job:" + job.id(), true);
			for (String raw : job.body().split("\\R")) {
				String text = clean(raw);
				add(lines, seen, "job", job.id(), label, text, true, group(text, "job:" + job.id(), projectNames),
						false);
			}
		}
		for (EducationResponse school : library.schools()) {
			String degree = (school.credential() + " " + school.fieldOfStudy()).strip();
			String text = degree.isEmpty() ? school.institution() : degree + ", " + school.institution();
			add(lines, seen, "school", school.id(), school.institution(), clean(text), true, "school:" + school.id(),
					true);
		}
		Map<String, ProjectLine> projectLines = new LinkedHashMap<>();
		for (ClaimView claim : library.claims()) {
			if (claim.planned()) {
				continue;
			}
			String text = clean("feature".equals(claim.kind()) ? claim.name() : claim.quote());
			if (text.isEmpty()) {
				continue;
			}
			projectLines
				.computeIfAbsent(claim.projectId() + "|" + Excerpts.normalize(text),
						key -> new ProjectLine(claim.projectId(), text))
				.add(claim);
		}
		for (ProjectLine line : projectLines.values()) {
			if (line.shown()) {
				add(lines, seen, "project", line.projectId, library.projectName(line.projectId), line.text,
						line.confirmed(), "project:" + line.projectId, false);
			}
		}
		return new EvidenceIndex(lines);
	}

	/**
	 * The lines a resume shows: headings, the lines whose keys are given, and each row of its skills section. A skills
	 * row is one source of its own, so listing a skill never counts as a second example of the work behind it.
	 */
	EvidenceIndex onResume(Set<String> keys, List<String> skillRows) {
		List<EvidenceLine> shown = new ArrayList<>(
				lines.stream().filter(line -> line.heading() || keys.contains(line.key())).toList());
		int number = 1;
		for (String row : skillRows) {
			shown.add(new EvidenceLine("S" + number++, "skills", SKILLS, "Skills section", row, true, "skills", true,
					true));
		}
		return new EvidenceIndex(shown);
	}

	List<EvidenceLine> lines() {
		return lines;
	}

	Optional<EvidenceLine> ref(String ref) {
		return Optional.ofNullable(byRef.get(ref));
	}

	/** The current line of the cited job, project, or school that still holds the excerpt, confirmed ones first. */
	Optional<EvidenceLine> holding(EvidenceRef ref) {
		return bySource.getOrDefault(source(ref.kind(), ref.sourceId()), List.of())
			.stream()
			.filter(line -> Excerpts.in(ref.excerpt(), line.text()))
			.min(Comparator.comparing(line -> !line.confirmed()));
	}

	/** Changes when a line is added, edited, or removed; not when a project line is confirmed. */
	String fingerprint() {
		return hash(lines.stream().map(EvidenceLine::key).sorted().toList());
	}

	static String hash(List<String> parts) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256")
				.digest(String.join("\n", parts).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash, 0, 8);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static void add(List<EvidenceLine> lines, Set<String> seen, String kind, UUID sourceId, String label,
			String text, boolean confirmed, String group, boolean heading) {
		if (lines.size() >= MAX_LINES || text.isEmpty() || text.split(" ").length < 2) {
			return;
		}
		boolean whole = text.length() <= LINE_LIMIT;
		String limited = whole ? text : text.substring(0, LINE_LIMIT).strip();
		if (seen.add(EvidenceLine.key(kind, sourceId, limited))) {
			lines.add(new EvidenceLine("E" + (lines.size() + 1), kind, sourceId, label, limited, confirmed, group,
					heading, whole));
		}
	}

	/** A job line that names one of the person's projects is about that project, not separate work. */
	private static String group(String text, String fallback, Map<UUID, Pattern> projectNames) {
		return projectNames.entrySet()
			.stream()
			.filter(entry -> entry.getValue().matcher(text).find())
			.map(entry -> "project:" + entry.getKey())
			.findFirst()
			.orElse(fallback);
	}

	private static Map<UUID, Pattern> projectPatterns(Library library) {
		Map<UUID, Pattern> patterns = new LinkedHashMap<>();
		library.projectNames()
			.entrySet()
			.stream()
			.filter(entry -> entry.getValue().strip().length() >= 4)
			.sorted(Comparator.<Map.Entry<UUID, String>>comparingInt(entry -> -entry.getValue().length())
				.thenComparing(entry -> entry.getKey().toString()))
			.forEach(entry -> patterns.put(entry.getKey(), Pattern.compile(
					"(?<![A-Za-z0-9])" + Pattern.quote(entry.getValue().strip()) + "(?![A-Za-z0-9])",
					Pattern.CASE_INSENSITIVE)));
		return patterns;
	}

	private static String clean(String text) {
		return BULLET.matcher(text).replaceFirst("").replaceAll("\\s+", " ").strip();
	}

	private static String source(String kind, UUID sourceId) {
		return kind + "|" + sourceId;
	}

	/**
	 * One project line and the claims on it. A line holding a bullet is confirmed once the bullet is kept and gone once
	 * it is marked not mine; the skills it names are kept or not on their own. A line only skills quote is confirmed once
	 * each of them that is not rejected is kept.
	 */
	private static final class ProjectLine {

		private final UUID projectId;

		private final String text;

		private final List<ClaimView> bullets = new ArrayList<>();

		private final List<ClaimView> skills = new ArrayList<>();

		private ProjectLine(UUID projectId, String text) {
			this.projectId = projectId;
			this.text = text;
		}

		private void add(ClaimView claim) {
			("feature".equals(claim.kind()) ? bullets : skills).add(claim);
		}

		private boolean shown() {
			return bullets.isEmpty() ? skills.stream().anyMatch(claim -> !rejected(claim))
					: bullets.stream().noneMatch(ProjectLine::rejected);
		}

		private boolean confirmed() {
			return (bullets.isEmpty() ? skills : bullets).stream()
				.filter(claim -> !rejected(claim))
				.allMatch(ClaimView::approved);
		}

		private static boolean rejected(ClaimView claim) {
			return !claim.approved() && !claim.candidate();
		}

	}

}
