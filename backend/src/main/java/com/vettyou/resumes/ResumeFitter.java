package com.vettyou.resumes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.vettyou.matching.RankedLine;
import com.vettyou.matching.RankedProject;
import com.vettyou.matching.ResumePlan;
import com.vettyou.matching.ResumeSkill;
import com.vettyou.profile.EducationResponse;
import com.vettyou.profile.ExperienceText;
import com.vettyou.profile.ProfileResponse;
import com.vettyou.templates.AtsFirstTemplate;
import com.vettyou.templates.ResumeDocument;

/**
 * Fits a plan on one page. Every job and school is printed. Then, each only if the page still fits: lines that cover
 * the posting, best first; a first line for each job that has none; the projects that show the most of the posting, up
 * to three with two lines each, even where jobs show the same; the other kept skills; and a second line for each job.
 * A job is filled with lines that still show something the posting asks for first, then the best-worded. A line is
 * never shortened or reworded; it goes on whole or stays off, so a line that reads as a note is never printed. Projects
 * print in the order they were ranked.
 */
@Component
class ResumeFitter {

	static final String SPACE = "space";

	static final String NOT_NEEDED = "not_needed";

	static final String TOO_LONG = "too_long";

	static final String NOTE = "note";

	private static final int FILL = 2;

	private static final Map<String, String> ROWS = rows();

	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

	private static final Comparator<ExperienceText> NEWEST_JOB_FIRST = Comparator
		.comparing((ExperienceText job) -> job.startDate() == null)
		.thenComparing(job -> !job.currentRole())
		.thenComparing(ResumeFitter::recency, Comparator.nullsLast(Comparator.reverseOrder()));

	private static final Comparator<EducationResponse> NEWEST_SCHOOL_FIRST = Comparator.comparing(
			(EducationResponse school) -> school.endDate() != null ? school.endDate() : school.startDate(),
			Comparator.nullsLast(Comparator.reverseOrder()));

	private final AtsFirstTemplate template;

	ResumeFitter(AtsFirstTemplate template) {
		this.template = template;
	}

	record Omitted(RankedLine line, String reason) {
	}

	/**
	 * {@code placed} are the lines printed, best first. {@code skillRows} are the skills section's lines as printed,
	 * for scoring.
	 */
	record Fit(ResumeDocument document, List<RankedLine> placed, List<Omitted> leftOff, List<ResumeSkill> skills,
			List<ResumeSkill> skillsLeftOff, List<String> skillRows) {

		Set<String> keys() {
			return placed.stream().map(RankedLine::key).collect(Collectors.toSet());
		}

	}

	Fit fit(ResumePlan plan) {
		Draft draft = new Draft(plan);
		plan.skills().stream().filter(ResumeSkill::asked).forEach(draft.skills::add);
		int limit = Math.max(1, template.pages(draft.document()));
		Map<String, Omitted> leftOff = new LinkedHashMap<>();
		for (RankedLine line : plan.lines()) {
			if (!line.whole()) {
				leftOff.put(line.key(), new Omitted(line, TOO_LONG));
			}
			else if (RankedLine.noteLike(line.text())) {
				leftOff.put(line.key(), new Omitted(line, NOTE));
			}
			else if (line.gain() > 0 && !draft.add(line, limit)) {
				leftOff.put(line.key(), new Omitted(line, SPACE));
			}
		}
		List<RankedLine> fillers = plan.lines()
			.stream()
			.filter(line -> "job".equals(line.kind()) && !draft.has(line) && !leftOff.containsKey(line.key()))
			.sorted(Comparator.comparing((RankedLine line) -> line.backs().isEmpty()))
			.toList();
		fill(draft, fillers, 1, limit, leftOff);
		Map<String, RankedLine> byKey = new HashMap<>();
		plan.lines().forEach(line -> byKey.put(line.key(), line));
		int shown = 0;
		for (RankedProject project : plan.projects()) {
			if (shown == ResumePlan.PROJECTS) {
				break;
			}
			for (String key : project.keys()) {
				if (draft.count("project", project.id()) >= ResumePlan.PROJECT_LINES) {
					break;
				}
				RankedLine line = byKey.get(key);
				if (line == null || draft.has(line) || leftOff.containsKey(key)) {
					continue;
				}
				if (!draft.add(line, limit)) {
					leftOff.put(key, new Omitted(line, SPACE));
				}
			}
			if (draft.count("project", project.id()) > 0) {
				shown++;
			}
		}
		List<ResumeSkill> skillsLeftOff = new ArrayList<>();
		for (ResumeSkill skill : plan.skills()) {
			if (!skill.asked() && !draft.add(skill, limit)) {
				skillsLeftOff.add(skill);
			}
		}
		fill(draft, fillers, FILL, limit, leftOff);
		for (RankedLine line : plan.lines()) {
			if (!draft.has(line) && !leftOff.containsKey(line.key())) {
				leftOff.put(line.key(), new Omitted(line, NOT_NEEDED));
			}
		}
		return new Fit(draft.document(), List.copyOf(draft.lines), List.copyOf(leftOff.values()),
				List.copyOf(draft.skills), List.copyOf(skillsLeftOff), draft.skillRows());
	}

	/** Adds lines, best first, to each job that shows fewer than {@code upTo}. */
	private static void fill(Draft draft, List<RankedLine> fillers, int upTo, int limit,
			Map<String, Omitted> leftOff) {
		for (RankedLine line : fillers) {
			if (draft.has(line) || leftOff.containsKey(line.key()) || draft.count("job", line.sourceId()) >= upTo) {
				continue;
			}
			if (!draft.add(line, limit)) {
				leftOff.put(line.key(), new Omitted(line, SPACE));
			}
		}
	}

	/** "Jan 2023 – Present", "Jan 2019 – Jun 2021", "Jan 2019", or empty. */
	static String dates(LocalDate start, LocalDate end, boolean current) {
		if (start == null) {
			return end == null ? "" : MONTH.format(end);
		}
		if (current) {
			return MONTH.format(start) + " \u2013 Present";
		}
		return end == null ? MONTH.format(start) : MONTH.format(start) + " \u2013 " + MONTH.format(end);
	}

	private static LocalDate recency(ExperienceText job) {
		if (job.currentRole() || job.endDate() == null) {
			return job.startDate();
		}
		return job.endDate();
	}

	private static String text(String value) {
		return value == null ? "" : value.strip();
	}

	private static String joined(String... parts) {
		return Arrays.stream(parts)
			.map(ResumeFitter::text)
			.filter(part -> !part.isEmpty())
			.collect(Collectors.joining(", "));
	}

	private static Map<String, String> rows() {
		Map<String, String> rows = new LinkedHashMap<>();
		rows.put("language", "Languages");
		rows.put("framework", "Frameworks");
		rows.put("database", "Databases");
		rows.put("platform", "Cloud and platforms");
		rows.put("tool", "Tools");
		rows.put("practice", "Practices");
		return rows;
	}

	private final class Draft {

		private final ResumePlan plan;

		private final List<RankedLine> lines = new ArrayList<>();

		private final List<ResumeSkill> skills = new ArrayList<>();

		private Draft(ResumePlan plan) {
			this.plan = plan;
		}

		private boolean add(RankedLine line, int limit) {
			lines.add(line);
			if (template.pages(document()) <= limit) {
				return true;
			}
			lines.remove(lines.size() - 1);
			return false;
		}

		private boolean add(ResumeSkill skill, int limit) {
			skills.add(skill);
			if (template.pages(document()) <= limit) {
				return true;
			}
			skills.remove(skills.size() - 1);
			return false;
		}

		private boolean has(RankedLine line) {
			return lines.stream().anyMatch(placed -> placed.key().equals(line.key()));
		}

		private long count(String kind, UUID sourceId) {
			return lines.stream().filter(line -> kind.equals(line.kind()) && line.sourceId().equals(sourceId)).count();
		}

		private List<ResumeDocument.Row> rows() {
			Map<String, List<String>> byRow = new LinkedHashMap<>();
			ROWS.values().forEach(label -> byRow.put(label, new ArrayList<>()));
			for (ResumeSkill skill : skills) {
				byRow.computeIfAbsent(ROWS.getOrDefault(skill.category(), "Other"), key -> new ArrayList<>())
					.add(skill.shown());
			}
			return byRow.entrySet()
				.stream()
				.filter(entry -> !entry.getValue().isEmpty())
				.map(entry -> new ResumeDocument.Row(entry.getKey(), String.join(", ", entry.getValue())))
				.toList();
		}

		private List<String> skillRows() {
			return rows().stream().map(row -> row.label() + ": " + row.text()).toList();
		}

		private ResumeDocument document() {
			ProfileResponse profile = plan.profile();
			List<ResumeDocument.Section> sections = new ArrayList<>();
			List<ResumeDocument.Row> rows = rows();
			if (!rows.isEmpty()) {
				sections.add(new ResumeDocument.Section("Skills", rows, List.of()));
			}
			List<ResumeDocument.Entry> jobs = plan.jobs()
				.stream()
				.sorted(NEWEST_JOB_FIRST)
				.map(job -> {
					String title = text(job.title()).isEmpty() ? text(job.employer()) : text(job.title());
					String subtitle = text(job.title()).isEmpty() ? text(job.location())
							: joined(job.employer(), job.location());
					return new ResumeDocument.Entry(title, dates(job.startDate(), job.endDate(), job.currentRole()),
							subtitle, bullets("job", job.id()));
				})
				.toList();
			if (!jobs.isEmpty()) {
				sections.add(new ResumeDocument.Section("Experience", List.of(), jobs));
			}
			List<UUID> ranking = plan.projects().stream().map(RankedProject::id).toList();
			List<ResumeDocument.Entry> projects = lines.stream()
				.filter(line -> "project".equals(line.kind()))
				.map(RankedLine::sourceId)
				.distinct()
				.sorted(Comparator.comparingInt((UUID id) -> ranking.contains(id) ? ranking.indexOf(id)
						: Integer.MAX_VALUE))
				.map(id -> new ResumeDocument.Entry(plan.projectNames().getOrDefault(id, "Project"), "", "",
						bullets("project", id)))
				.toList();
			if (!projects.isEmpty()) {
				sections.add(new ResumeDocument.Section("Projects", List.of(), projects));
			}
			List<ResumeDocument.Entry> schools = plan.schools().stream().sorted(NEWEST_SCHOOL_FIRST).map(school -> {
				String degree = joinedWords(school.credential(), school.fieldOfStudy());
				return new ResumeDocument.Entry(degree.isEmpty() ? text(school.institution()) : degree,
						dates(school.startDate(), school.endDate(), false),
						degree.isEmpty() ? "" : text(school.institution()), List.of());
			}).toList();
			if (!schools.isEmpty()) {
				sections.add(new ResumeDocument.Section("Education", List.of(), schools));
			}
			List<String> contact = List.of(text(profile.email()), text(profile.phone()), text(profile.location()),
					text(profile.website()), text(profile.linkedin()));
			return new ResumeDocument(text(profile.displayName()),
					contact.stream().filter(item -> !item.isEmpty()).toList(), sections);
		}

		private List<String> bullets(String kind, UUID sourceId) {
			return lines.stream()
				.filter(line -> kind.equals(line.kind()) && line.sourceId().equals(sourceId))
				.map(RankedLine::text)
				.toList();
		}

		private String joinedWords(String first, String second) {
			return (text(first) + " " + text(second)).strip();
		}

	}

}
