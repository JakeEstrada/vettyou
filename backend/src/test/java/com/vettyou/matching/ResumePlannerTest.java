package com.vettyou.matching;

import static com.vettyou.matching.MatchFixtures.LIMINNALITY;
import static com.vettyou.matching.MatchFixtures.NANOHUB;
import static com.vettyou.matching.MatchFixtures.USER;
import static com.vettyou.matching.MatchFixtures.confirmed;
import static com.vettyou.matching.MatchFixtures.feature;
import static com.vettyou.matching.MatchFixtures.item;
import static com.vettyou.matching.MatchFixtures.jake;
import static com.vettyou.matching.MatchFixtures.judged;
import static com.vettyou.matching.MatchFixtures.nurse;
import static com.vettyou.matching.MatchFixtures.service;
import static com.vettyou.matching.MatchFixtures.stored;
import static com.vettyou.matching.MatchFixtures.withClaims;
import static com.vettyou.matching.MatchFixtures.withProfile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.vettyou.evidence.ClaimView;
import com.vettyou.profile.ProfileResponse;

/** Ranking a person's lines for a posting, and scoring what one resume shows. */
class ResumePlannerTest {

	private static ResumePlan plan(Library library, List<RequirementInput> posting, String text) {
		return service(library, "{}").plan(USER, posting, "", text);
	}

	private static String key(Library library, String start) {
		return EvidenceIndex.of(library)
			.lines()
			.stream()
			.filter(line -> line.text().startsWith(start))
			.findFirst()
			.orElseThrow(() -> new AssertionError("No line starts with: " + start))
			.key();
	}

	private static RankedLine line(ResumePlan plan, String start) {
		return plan.lines()
			.stream()
			.filter(line -> line.text().startsWith(start))
			.findFirst()
			.orElseThrow(() -> new AssertionError("No ranked line starts with: " + start));
	}

	private static ResumeSkill skill(ResumePlan plan, String name) {
		return plan.skills()
			.stream()
			.filter(skill -> skill.name().equals(name))
			.findFirst()
			.orElseThrow(() -> new AssertionError("No skill: " + name));
	}

	@Test
	void linesThatCoverMoreOfThePostingComeFirst() {
		Library library = jake();
		List<RequirementInput> posting = List.of(
				stored("qualification", "required", "all", "Build REST APIs for production systems",
						judged(library, "REST APIs", "rest apis", "direct", "Designed REST APIs"),
						judged(library, "production systems", "production systems", "direct",
								"Designed, architected, and deployed production systems")),
				stored("qualification", "required", "all", "Python scripting",
						judged(library, "Python scripting", "python", "direct", "Wrote a Python script")));

		ResumePlan plan = plan(library, posting, "");

		assertThat(plan.lines().get(0).text()).startsWith("Wrote a Python script");
		assertThat(plan.lines().get(0).gain()).isEqualTo(0.5);
		assertThat(plan.lines().get(0).backs())
			.containsExactly(new LineBacking("Python scripting", "Python scripting", "required", "direct"));
		assertThat(plan.lines().subList(1, 3)).extracting(RankedLine::gain).containsExactly(0.25, 0.25);
		assertThat(plan.lines().subList(3, plan.lines().size())).allSatisfy(line -> assertThat(line.gain()).isZero());
		assertThat(plan.lines().stream().mapToDouble(RankedLine::gain).sum()).isCloseTo(1.0, within(1e-9));
	}

	@Test
	void aSecondLineForAPartAlreadyCoveredAddsNothing() {
		Library library = jake();
		List<RequirementInput> posting = List.of(stored("qualification", "required", "all", "Automation",
				judged(library, "automation", "automation", "direct",
						"Built spreadsheets, quoting tools, and other automation",
						"develop custom software that automates administrative processes")));

		ResumePlan plan = plan(library, posting, "");

		double first = line(plan, "Built spreadsheets, quoting tools").gain();
		double second = line(plan, "Work directly with businesses").gain();
		assertThat(List.of(first, second)).containsExactlyInAnyOrder(1.0, 0.0);
		assertThat(line(plan, "Work directly with businesses").backs()).hasSize(1);
	}

	@Test
	void onlyConfirmedLinesAreRankedAndUnconfirmedOnesThatWouldHelpAreListed() {
		Library library = jake();
		List<RequirementInput> posting = List.of(
				item("qualification", "required", "all", "framework", "Angular", "Angular", "angular"));

		ResumePlan plan = plan(library, posting, "Angular");

		assertThat(plan.lines()).allSatisfy(line -> {
			assertThat(line.confirmed()).isTrue();
			assertThat(line.kind()).isEqualTo("job");
		});
		assertThat(plan.toConfirm()).singleElement().satisfies(line -> {
			assertThat(line.text()).isEqualTo("Admin app built with Angular and a REST API");
			assertThat(line.confirmed()).isFalse();
			assertThat(line.gain()).isEqualTo(1.0);
		});
		assertThat(plan.skills()).extracting(ResumeSkill::name).doesNotContain("Angular");
	}

	@Test
	void projectsAreRankedByWhatTheirOwnLinesShowEvenWhereJobsShowTheSame() {
		Library base = confirmed(jake());
		List<ClaimView> claims = new ArrayList<>(base.claims());
		claims.add(feature(LIMINNALITY, "Vitest \u00b7 Node test runner \u00b7 GitHub Actions", "approved"));
		Library library = withClaims(base, claims);
		List<RequirementInput> posting = List.of(
				stored("qualification", "required", "all", "Test the UI and the API",
						judged(library, "UI tests", "vitest", "direct", "Cover the UI with Vitest",
								"Frontend tests run in Vitest", "Vitest \u00b7 Node test runner"),
						judged(library, "API tests", "node test runner", "direct", "the API with Node's test runner")),
				stored("qualification", "required", "all", "Angular",
						judged(library, "Angular", "angular", "direct", "Admin app built with Angular")));

		ResumePlan plan = plan(library, posting, "");

		assertThat(line(plan, "Frontend tests run in Vitest").gain()).isZero();
		assertThat(plan.projects()).extracting(RankedProject::name, RankedProject::share)
			.containsExactly(tuple("NanoHub", 0.5), tuple("Liminnality", 0.25));
		assertThat(plan.projects().get(0).keys()).containsExactly(key(library, "Admin app built with Angular"));
		assertThat(plan.projects().get(1).keys()).containsExactly(key(library, "Frontend tests run in Vitest"));
		assertThat(plan.toConfirm()).isEmpty();
	}

	@Test
	void aLineThatReadsAsANoteNeverCoversAndIsNeverAskedFor() {
		Library base = confirmed(jake());
		List<ClaimView> claims = new ArrayList<>(base.claims());
		claims.add(feature(NANOHUB, "One resume says the admin app used Angular Material", "approved"));
		claims.add(feature(NANOHUB, "Older resumes also say the catalog ran on Kubernetes", "candidate"));
		Library library = withClaims(base, claims);
		List<RequirementInput> posting = List.of(
				stored("qualification", "required", "all", "Angular Material",
						judged(library, "Angular Material", "angular material", "direct", "used Angular Material")),
				stored("qualification", "required", "all", "Kubernetes",
						judged(library, "Kubernetes", "kubernetes", "direct", "the catalog ran on Kubernetes")));

		ResumePlan plan = plan(library, posting, "");

		assertThat(line(plan, "One resume says the admin app").gain()).isZero();
		assertThat(plan.lines()).allSatisfy(line -> assertThat(line.gain()).isZero());
		assertThat(plan.projects()).isEmpty();
		assertThat(plan.toConfirm()).isEmpty();
	}

	@Test
	void linesThatWouldPutAProjectOnTheResumeAreListedToConfirm() {
		Library library = jake();
		List<RequirementInput> posting = List.of(
				stored("qualification", "required", "all", "Test the UI",
						judged(library, "UI tests", "vitest", "direct", "Cover the UI with Vitest",
								"Frontend tests run in Vitest")),
				stored("qualification", "required", "all", "Angular",
						judged(library, "Angular", "angular", "direct", "Admin app built with Angular")));

		ResumePlan plan = plan(library, posting, "");

		assertThat(plan.projects()).isEmpty();
		assertThat(plan.toConfirm()).extracting(RankedLine::text, RankedLine::gain)
			.containsExactly(tuple("Admin app built with Angular and a REST API", 0.5),
					tuple("Frontend tests run in Vitest. API tests run in Node's test runner", 0.0));
	}

	@Test
	void skillsUseThePostingsSpellingOnlyForTheSameProduct() {
		ResumePlan plan = plan(confirmed(jake()), List.of(),
				"We use Postgres and ReactJS next to an AngularJS app. SQL Server, ASP.NET Core, and Scrum are a plus.");

		assertThat(skill(plan, "PostgreSQL").shown()).isEqualTo("Postgres");
		assertThat(skill(plan, "PostgreSQL").asked()).isTrue();
		assertThat(skill(plan, "React").shown()).isEqualTo("ReactJS");
		assertThat(skill(plan, "Angular").shown()).isEqualTo("Angular");
		assertThat(skill(plan, "Angular").asked()).isFalse();
		assertThat(skill(plan, "SQL").shown()).isEqualTo("SQL");
		assertThat(skill(plan, "SQL").asked()).isFalse();
		assertThat(plan.skills()).extracting(ResumeSkill::name).doesNotContain("SQL Server", ".NET", "Agile");
		assertThat(plan.skills().subList(0, 2)).extracting(ResumeSkill::name).containsExactly("PostgreSQL", "React");
		assertThat(plan.skills().subList(2, plan.skills().size())).noneMatch(ResumeSkill::asked);
	}

	@Test
	void sourcesChangeWithAnythingAResumeWouldPrintOrCount() {
		String before = plan(jake(), List.of(), "").sources();

		assertThat(plan(jake(), List.of(), "Different posting text").sources()).isEqualTo(before);
		assertThat(plan(confirmed(jake()), List.of(), "").sources()).isNotEqualTo(before);
		assertThat(plan(withProfile(jake(),
				new ProfileResponse("Jake Estrada", "jake@example.com", "", "Lake Forest, CA", "", "")), List.of(), "")
			.sources()).isNotEqualTo(before);
	}

	@Test
	void evidenceLeftOffTheResumeReadsHiddenAndEarnsNothing() {
		Library library = jake();
		List<RequirementInput> posting = List.of(stored("qualification", "required", "all", "Python scripting",
				judged(library, "Python scripting", "python", "direct", "Wrote a Python script")));
		MatchService service = service(library, "{}");

		RequirementMatch hidden = service.scoreResume(USER, posting, Set.of(), List.of(), 1.0).requirements().get(0);
		RequirementMatch shown = service
			.scoreResume(USER, posting, Set.of(key(library, "Wrote a Python script")), List.of(), 1.0)
			.requirements()
			.get(0);

		assertThat(hidden.status()).isEqualTo("hidden");
		assertThat(hidden.credit()).isZero();
		assertThat(hidden.parts().get(0).status()).isEqualTo("hidden");
		assertThat(hidden.note()).contains("none of them is on this resume");
		assertThat(shown.status()).isEqualTo("direct");
		assertThat(shown.credit()).isEqualTo(1.0);
	}

	@Test
	void aSkillsRowShowsATechnologyButIsNotRatedAsWork() {
		Library library = jake();
		List<RequirementInput> posting = List.of(stored("qualification", "required", "all", "PostgreSQL",
				judged(library, "PostgreSQL", "postgresql", "direct", "Use SQL against PostgreSQL")));

		MatchResult result = service(library, "{}").scoreResume(USER, posting, Set.of(),
				List.of("Databases: PostgreSQL, MongoDB"), 1.0);

		RequirementMatch postgres = result.requirements().get(0);
		assertThat(postgres.status()).isEqualTo("direct");
		assertThat(postgres.parts().get(0).evidence()).singleElement()
			.satisfies(cite -> assertThat(cite.kind()).isEqualTo("skills"));
		assertThat(postgres.strength()).isNull();
	}

	@Test
	void readabilityWeighsInProportionToCoverage() {
		Library library = jake();
		List<RequirementInput> posting = List.of(
				stored("qualification", "required", "all", "Python scripting",
						judged(library, "Python scripting", "python", "direct", "Wrote a Python script")),
				stored("qualification", "required", "all", "Rust", judged(library, "Rust", "rust", "none")));

		MatchScore score = service(library, "{}")
			.scoreResume(USER, posting, Set.of(key(library, "Wrote a Python script")), List.of(), 1.0)
			.score();

		ScoreFactor readability = score.factors()
			.stream()
			.filter(factor -> factor.key().equals("readability"))
			.findFirst()
			.orElseThrow();
		assertThat(readability.value()).isEqualTo(1.0);
		assertThat(readability.activeWeight()).isEqualTo(7.5);
		assertThat(score.formula()).contains("Readability weight: 15 x 0.5000 coverage = 7.5.");
		assertThat(score.total()).isEqualTo(score.totalIfConfirmed());
		assertThat(score.label()).isEqualTo("Vettyou estimate");
		assertThat(score.note()).startsWith("Our own estimate of how well this resume covers the posting.")
			.contains("not an employer's ATS score");
	}

	@Test
	void aPerfectlyReadableResumeCannotCarryAJobThePersonDoesNotFit() {
		Library library = nurse();
		List<RequirementInput> posting = List.of(
				item("qualification", "required", "all", "language", "Python", "Python", "python"),
				item("qualification", "required", "all", "framework", "React", "React", "react"));

		MatchScore score = service(library, "{}").scoreResume(USER, posting, Set.of(), List.of(), 1.0).score();

		assertThat(score.total()).isZero();
		assertThat(score.label()).isEqualTo("Vettyou estimate");
	}

}
