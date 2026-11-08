package com.vettyou.resumes;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.vettyou.matching.LineBacking;
import com.vettyou.matching.MatchResult;
import com.vettyou.matching.RankedLine;
import com.vettyou.matching.RankedProject;
import com.vettyou.matching.ResumePlan;
import com.vettyou.matching.ResumeSkill;
import com.vettyou.profile.EducationResponse;
import com.vettyou.profile.ExperienceText;
import com.vettyou.profile.ProfileResponse;
import com.vettyou.templates.AtsFirstTemplate;
import com.vettyou.templates.ResumeDocument;

class ResumeFitterTest {

	static final UUID CURRENT = UUID.randomUUID();

	static final UUID PAST = UUID.randomUUID();

	static final UUID OLDEST = UUID.randomUUID();

	static final UUID UNDATED = UUID.randomUUID();

	static final UUID PROJECT = UUID.randomUUID();

	static final ProfileResponse ADA = new ProfileResponse("Ada Lovelace", "ada@example.com", "(555) 010-0100",
			"London", "", "");

	private final AtsFirstTemplate template = new AtsFirstTemplate();

	private final ResumeFitter fitter = new ResumeFitter(template);

	static ResumePlan plan(ProfileResponse profile, List<RankedLine> lines, List<ResumeSkill> skills) {
		return plan(profile, lines, List.of(), skills);
	}

	static ResumePlan plan(ProfileResponse profile, List<RankedLine> lines, List<RankedProject> projects,
			List<ResumeSkill> skills) {
		Map<UUID, String> names = new HashMap<>(Map.of(PROJECT, "Pantry"));
		projects.forEach(project -> names.put(project.id(), project.name()));
		return new ResumePlan(new MatchResult(List.of(), null, false), lines, projects, List.of(), skills, "sources",
				profile,
				List.of(job(PAST, "Analyst", "Old Co", LocalDate.of(2018, 1, 1), LocalDate.of(2020, 6, 1), false),
						job(UNDATED, "Volunteer", "Chess Club", null, null, false),
						job(CURRENT, "Developer", "Acme", LocalDate.of(2023, 1, 1), null, true),
						job(OLDEST, "Clerk", "Shop", LocalDate.of(2015, 1, 1), LocalDate.of(2017, 12, 1), false)),
				List.of(new EducationResponse(UUID.randomUUID(), "Community College", "A.A.", "Mathematics", null,
						LocalDate.of(2014, 6, 1)),
						new EducationResponse(UUID.randomUUID(), "State University", "B.S.", "Computer Science",
								LocalDate.of(2018, 9, 1), LocalDate.of(2022, 6, 1))),
				Map.copyOf(names));
	}

	static ExperienceText job(UUID id, String title, String employer, LocalDate start, LocalDate end,
			boolean current) {
		return new ExperienceText(id, employer, title, "London", start, end, current, "");
	}

	static RankedLine line(String kind, UUID source, String text, double gain) {
		return new RankedLine(kind + "|" + source + "|" + text, kind, source, "label", text, true, true, gain, 0.6,
				List.of());
	}

	static RankedLine backed(String kind, UUID source, String text, double gain, LineBacking... backs) {
		return new RankedLine(kind + "|" + source + "|" + text, kind, source, "label", text, true, true, gain, 0.6,
				List.of(backs));
	}

	private static List<ResumeDocument.Entry> entries(ResumeDocument document, String heading) {
		return document.sections()
			.stream()
			.filter(section -> section.heading().equals(heading))
			.findFirst()
			.map(ResumeDocument.Section::entries)
			.orElse(List.of());
	}

	@Test
	void everyJobAndSchoolIsPrintedNewestFirstEvenWithoutLines() {
		ResumeFitter.Fit fit = fitter.fit(plan(ADA, List.of(), List.of()));

		assertThat(entries(fit.document(), "Experience")).extracting(ResumeDocument.Entry::title)
			.containsExactly("Developer", "Analyst", "Clerk", "Volunteer");
		assertThat(entries(fit.document(), "Experience").get(0).dates()).isEqualTo("Jan 2023 \u2013 Present");
		assertThat(entries(fit.document(), "Experience").get(0).subtitle()).isEqualTo("Acme, London");
		assertThat(entries(fit.document(), "Experience").get(1).dates()).isEqualTo("Jan 2018 \u2013 Jun 2020");
		assertThat(entries(fit.document(), "Education")).extracting(ResumeDocument.Entry::title)
			.containsExactly("B.S. Computer Science", "A.A. Mathematics");
		assertThat(fit.document().sections()).extracting(ResumeDocument.Section::heading)
			.containsExactly("Experience", "Education");
		assertThat(fit.document().name()).isEqualTo("Ada Lovelace");
		assertThat(fit.document().contact()).containsExactly("ada@example.com", "(555) 010-0100", "London");
	}

	@Test
	void linesGoOnWholeAndBestFirstUntilThePageIsFull() {
		List<RankedLine> lines = new ArrayList<>();
		IntStream.range(0, 60).forEach(i -> lines.add(line("job", CURRENT, "Line " + i + " shipped a feature that "
				+ "customers asked for, measured its effect on support tickets, and wrote down what changed for "
				+ "the team", 0.5 - i * 0.005)));

		ResumeFitter.Fit fit = fitter.fit(plan(ADA, lines, List.of()));

		assertThat(template.pages(fit.document())).isEqualTo(1);
		assertThat(fit.placed()).isNotEmpty().isEqualTo(lines.subList(0, fit.placed().size()));
		assertThat(entries(fit.document(), "Experience").get(0).bullets())
			.isEqualTo(fit.placed().stream().map(RankedLine::text).toList());
		assertThat(fit.leftOff()).hasSize(lines.size() - fit.placed().size())
			.allSatisfy(omitted -> assertThat(omitted.reason()).isEqualTo(ResumeFitter.SPACE));
	}

	@Test
	void aLineTooLongToPrintIsNeverPrinted() {
		RankedLine whole = line("job", CURRENT, "Built the billing service", 0.4);
		RankedLine cut = new RankedLine("job|cut", "job", CURRENT, "label", "A very long line", true, false, 0, 0.6,
				List.of());

		ResumeFitter.Fit fit = fitter.fit(plan(ADA, List.of(whole, cut), List.of()));

		assertThat(fit.placed()).containsExactly(whole);
		assertThat(fit.leftOff()).singleElement().satisfies(omitted -> {
			assertThat(omitted.line()).isEqualTo(cut);
			assertThat(omitted.reason()).isEqualTo(ResumeFitter.TOO_LONG);
		});
	}

	@Test
	void linesThatAddNothingOnlyFillEachJobToTwo() {
		List<RankedLine> lines = List.of(line("job", CURRENT, "Current one", 0), line("job", CURRENT, "Current two", 0),
				line("job", CURRENT, "Current three", 0), line("job", PAST, "Past one", 0),
				line("project", PROJECT, "Pantry one", 0));

		ResumeFitter.Fit fit = fitter.fit(plan(ADA, lines, List.of()));

		assertThat(fit.placed()).extracting(RankedLine::text).containsExactly("Current one", "Past one", "Current two");
		assertThat(entries(fit.document(), "Experience").get(0).bullets()).containsExactly("Current one",
				"Current two");
		assertThat(fit.leftOff()).extracting(omitted -> omitted.line().text() + ":" + omitted.reason())
			.containsExactly("Current three:not_needed", "Pantry one:not_needed");
		assertThat(entries(fit.document(), "Projects")).isEmpty();
	}

	@Test
	void everyJobGetsAFirstLineBeforeProjectsAndASecondAfter() {
		LineBacking react = new LineBacking("React", "React", "required", "direct");
		RankedLine covers = backed("job", CURRENT, "Built React screens for the billing team", 0.5, react);
		RankedLine currentSecond = line("job", CURRENT, "Answered support tickets", 0);
		RankedLine pastFirst = line("job", PAST, "Reconciled the monthly ledger", 0);
		RankedLine pastSecond = line("job", PAST, "Trained two new analysts", 0);
		RankedLine pantry = backed("project", PROJECT, "Scans barcodes into a shared React pantry", 0, react);

		ResumeFitter.Fit fit = fitter.fit(plan(ADA, List.of(covers, currentSecond, pastFirst, pastSecond, pantry),
				List.of(new RankedProject(PROJECT, "Pantry", 0.5, List.of(pantry.key()))), List.of()));

		assertThat(fit.placed()).containsExactly(covers, pastFirst, pantry, currentSecond, pastSecond);
	}

	@Test
	void fillingPrefersLinesThatStillShowThePostingAndNotesAreNeverPrinted() {
		RankedLine note = line("job", CURRENT,
				"Own practice \u00b7 example.com \u00b7 Older resumes also say Lead Developer and Engineer", 0);
		RankedLine plain = line("job", CURRENT, "Answered the phones", 0);
		RankedLine relevant = new RankedLine("job|relevant", "job", CURRENT, "label", "Wrote React screens", true, true,
				0, 0.6, List.of(new LineBacking("React", "React", "required", "direct")));
		RankedLine other = line("job", CURRENT, "Kept the office tidy", 0);

		ResumeFitter.Fit fit = fitter.fit(plan(ADA, List.of(note, plain, relevant, other), List.of()));

		assertThat(fit.placed()).extracting(RankedLine::text)
			.containsExactly("Wrote React screens", "Answered the phones");
		assertThat(fit.leftOff()).extracting(omitted -> omitted.line().text() + ":" + omitted.reason())
			.containsExactly(note.text() + ":note", "Kept the office tidy:not_needed");
		assertThat(RankedLine.noteLike("Built a CLI | a web app | an API")).isTrue();
		assertThat(RankedLine.noteLike("Cut build time from 9 to 3 minutes \u00b7 then documented it")).isFalse();
		assertThat(RankedLine.noteLike("Schemas with validation and indexing. Older resumes also say query "
				+ "performance was improved")).isTrue();
		assertThat(RankedLine.noteLike("An older resume also calls this a workforce tool")).isTrue();
		assertThat(RankedLine.noteLike("One older resume says part-time")).isTrue();
		assertThat(RankedLine.noteLike("Each tailored resume lists the lines it prints")).isFalse();
	}

	@Test
	void aNoteIsNeverPrintedEvenWhenItCoversThePosting() {
		RankedLine note = backed("job", CURRENT, "Older resumes also describe the API as C# and ASP.NET Core", 0.4,
				new LineBacking("C#", "C#", "required", "direct"));

		ResumeFitter.Fit fit = fitter.fit(plan(ADA, List.of(note), List.of()));

		assertThat(fit.placed()).isEmpty();
		assertThat(fit.leftOff()).extracting(omitted -> omitted.line().text() + ":" + omitted.reason())
			.containsExactly(note.text() + ":note");
	}

	@Test
	void projectsThatShowThePostingGoOnEvenWhenJobsShowTheSame() {
		LineBacking react = new LineBacking("React", "React", "required", "direct");
		RankedLine covers = backed("job", CURRENT, "Built React screens for the billing team", 0.5, react);
		RankedLine filler = line("job", CURRENT, "Answered support tickets", 0);
		RankedLine first = backed("project", PROJECT, "Scans barcodes into a shared React pantry", 0, react);
		RankedLine second = backed("project", PROJECT, "Its React screens keep working offline", 0, react);
		RankedLine third = backed("project", PROJECT, "Lists every shelf with React", 0, react);
		RankedProject pantry = new RankedProject(PROJECT, "Pantry", 0.5,
				List.of(first.key(), second.key(), third.key()));

		ResumeFitter.Fit fit = fitter
			.fit(plan(ADA, List.of(covers, filler, first, second, third), List.of(pantry), List.of()));

		assertThat(fit.placed()).containsExactly(covers, first, second, filler);
		assertThat(entries(fit.document(), "Projects")).singleElement().satisfies(entry -> {
			assertThat(entry.title()).isEqualTo("Pantry");
			assertThat(entry.bullets()).containsExactly(first.text(), second.text());
		});
		assertThat(fit.leftOff()).extracting(omitted -> omitted.line().text() + ":" + omitted.reason())
			.containsExactly(third.text() + ":not_needed");
	}

	@Test
	void upToThreeProjectsGoOnForTheirOwnSakeInTheOrderTheyRank() {
		LineBacking sql = new LineBacking("SQL", "SQL", "required", "direct");
		List<String> names = List.of("Atlas", "Beacon", "Cobalt", "Delta", "Ember");
		List<RankedLine> lines = new ArrayList<>();
		List<RankedProject> projects = new ArrayList<>();
		for (int i = 0; i < names.size(); i++) {
			UUID id = UUID.randomUUID();
			RankedLine line = backed("project", id, names.get(i) + " queries SQL tables", i == 3 ? 0.05 : 0, sql);
			lines.add(i == 3 ? 0 : lines.size(), line);
			projects.add(new RankedProject(id, names.get(i), 0.5 - i * 0.1, List.of(line.key())));
		}

		ResumeFitter.Fit fit = fitter.fit(plan(ADA, lines, projects, List.of()));

		assertThat(entries(fit.document(), "Projects")).extracting(ResumeDocument.Entry::title)
			.containsExactly("Atlas", "Beacon", "Cobalt", "Delta");
		assertThat(fit.leftOff()).extracting(omitted -> omitted.line().text() + ":" + omitted.reason())
			.containsExactly("Ember queries SQL tables:not_needed");
	}

	@Test
	void aProjectThatDoesNotFitIsLeftOffForSpaceOnce() {
		List<RankedLine> lines = new ArrayList<>();
		IntStream.range(0, 90).forEach(i -> lines.add(line("job", CURRENT, "Line " + i + " shipped a feature", 0.9 - i
				* 0.005)));
		RankedLine pantry = backed("project", PROJECT, "Scans barcodes into a shared pantry", 0,
				new LineBacking("Barcodes", "Barcodes", "required", "direct"));
		lines.add(pantry);

		ResumeFitter.Fit fit = fitter
			.fit(plan(ADA, lines, List.of(new RankedProject(PROJECT, "Pantry", 0.3, List.of(pantry.key()))), List.of()));

		assertThat(template.pages(fit.document())).isEqualTo(1);
		assertThat(entries(fit.document(), "Projects")).isEmpty();
		assertThat(fit.leftOff()).filteredOn(omitted -> omitted.line().equals(pantry))
			.singleElement()
			.extracting(ResumeFitter.Omitted::reason)
			.isEqualTo(ResumeFitter.SPACE);
	}

	@Test
	void aProjectAppearsOnceOneOfItsLinesCovers() {
		ResumeFitter.Fit fit = fitter
			.fit(plan(ADA, List.of(line("project", PROJECT, "Scans barcodes into a shared pantry", 0.2)), List.of()));

		assertThat(entries(fit.document(), "Projects")).singleElement().satisfies(entry -> {
			assertThat(entry.title()).isEqualTo("Pantry");
			assertThat(entry.bullets()).containsExactly("Scans barcodes into a shared pantry");
		});
	}

	@Test
	void skillsPrintAsKeptWithThePostingsSpelling() {
		List<ResumeSkill> skills = List.of(new ResumeSkill("PostgreSQL", "database", "Postgres", true, 1),
				new ResumeSkill("Java", "language", "Java", false, 2),
				new ResumeSkill("React", "framework", "React", false, 1));

		ResumeFitter.Fit fit = fitter.fit(plan(ADA, List.of(), skills));

		assertThat(fit.skillRows()).containsExactly("Languages: Java", "Frameworks: React", "Databases: Postgres");
		assertThat(fit.document().sections().get(0).heading()).isEqualTo("Skills");
		assertThat(fit.skillsLeftOff()).isEmpty();
	}

}
