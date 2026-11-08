package com.vettyou.matching;

import static com.vettyou.matching.MatchFixtures.item;
import static com.vettyou.matching.MatchFixtures.jake;
import static com.vettyou.matching.MatchFixtures.nurse;
import static com.vettyou.matching.MatchFixtures.part;
import static com.vettyou.matching.MatchFixtures.find;
import static com.vettyou.matching.MatchFixtures.run;
import static com.vettyou.matching.MatchFixtures.said;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.vettyou.matching.MatchFixtures.Said;

/**
 * Several people against several postings, with the model's answers written by hand. The bands are fixed by meaning,
 * not fitted to these numbers: every required item shown directly is a strong fit (75 or more), nothing relevant is
 * no fit (20 or less), and anything else sits between.
 */
class MatchCalibrationTest {

	static List<RequirementInput> fullStack() {
		return List.of(
				item("qualification", "required", "all", "experience",
						"3+ years building web applications with React and TypeScript.",
						"3+ years building web applications", "web application experience", "React", "React",
						"TypeScript", "TypeScript"),
				item("qualification", "required", "all", "experience", "Experience designing REST APIs with Node.js.",
						"designing REST APIs", "API design", "Node.js", "Node.js"),
				item("qualification", "required", "any", "tool", "PostgreSQL or another relational database.",
						"PostgreSQL", "PostgreSQL", "another relational database", "relational databases"),
				item("qualification", "required", "all", "practice", "Write automated tests for the code you ship.",
						"automated tests", "automated testing"),
				item("qualification", "required", "all", "soft_skill",
						"Work directly with customers to turn their needs into features.", "work directly with customers",
						"customer collaboration", "turn needs into features", "requirements translation"),
				item("responsibility", "required", "all", "practice", "Build and ship features end to end.",
						"build and ship features end to end", "end-to-end delivery"),
				item("qualification", "preferred", "all", "framework", "Experience with Angular.", "Angular", "Angular"),
				item("qualification", "preferred", "all", "tool", "AWS experience.", "AWS", "AWS"));
	}

	static List<RequirementInput> nursing() {
		return List.of(
				item("qualification", "required", "all", "credential", "Active California RN license.",
						"Active California RN license", "RN license"),
				item("qualification", "required", "all", "credential", "Current BLS certification.", "BLS certification",
						"BLS certification"),
				item("qualification", "required", "all", "experience", "2+ years of acute care experience.", "2+ years",
						"years of experience", "acute care", "acute care"),
				item("qualification", "required", "all", "tool", "Experience charting in Epic.", "charting in Epic",
						"EHR charting"),
				item("qualification", "required", "all", "practice", "Patient education and discharge planning.",
						"patient education", "patient education", "discharge planning", "discharge planning"),
				item("responsibility", "required", "all", "practice", "Provide direct care for telemetry patients.",
						"direct care for telemetry patients", "patient care"),
				item("responsibility", "required", "all", "practice", "Precept and support new nurses.",
						"precept new nurses", "precepting"),
				item("qualification", "preferred", "all", "education", "BSN preferred.", "BSN", "BSN"));
	}

	static List<RequirementInput> dataEngineering() {
		return List.of(
				item("qualification", "required", "all", "practice", "Build batch and streaming data pipelines.",
						"batch data pipelines", "data pipelines", "streaming data pipelines", "streaming"),
				item("qualification", "required", "any", "tool", "Hands-on with Spark or Kafka.", "Spark", "Spark",
						"Kafka", "Kafka"),
				item("qualification", "required", "all", "practice", "Strong SQL and data modeling skills.", "SQL", "SQL",
						"data modeling", "data modeling"),
				item("qualification", "required", "all", "language", "Python for data processing.", "Python", "Python",
						"data processing", "data processing"),
				item("qualification", "required", "any", "tool", "Experience with Airflow or dbt.", "Airflow", "Airflow",
						"dbt", "dbt"),
				item("responsibility", "required", "all", "practice", "Monitor pipeline reliability and data quality.",
						"pipeline reliability", "pipeline reliability", "data quality", "data quality"),
				item("qualification", "preferred", "all", "practice", "Experience with ML feature pipelines.",
						"ML feature pipelines", "ML data"));
	}

	private static final Said[] JAKE_FULL_STACK = { said("R1.1", "unclear"),
			said("R1.2", "direct", "TypeScript, React, Node.js"), said("R1.3", "direct", "using TypeScript, React"),
			said("R2.1", "direct", "Designed REST APIs"), said("R2.2", "direct", "React, Node.js, Express"),
			said("R3.1", "direct", "SQL against PostgreSQL"), said("R3.2", "none"),
			said("R4.1", "direct", "Cover the UI with Vitest and the API with Node's test runner"),
			said("R5.1", "direct", "Work directly with businesses to understand their operations"),
			said("R5.2", "direct", "Translated operator and office needs into product requirements"),
			said("R6.1", "direct", "Owned systems from requirements discovery through architecture, deployment"),
			said("R7.1", "none"), said("R8.1", "none") };

	private static final Said[] NONE_FULL_STACK = { said("R1.1", "none"), said("R1.2", "none"), said("R1.3", "none"),
			said("R2.1", "none"), said("R2.2", "none"), said("R3.1", "none"), said("R3.2", "none"), said("R4.1", "none"),
			said("R5.1", "none"), said("R5.2", "none"), said("R6.1", "none"), said("R7.1", "none"),
			said("R8.1", "none") };

	private static final Said[] NURSE_NURSING = { said("R1.1", "unclear", "Registered Nurse"),
			said("R2.1", "unclear"), said("R3.1", "unclear"),
			said("R3.2", "direct", "Provide bedside care for 5 to 6 telemetry patients per shift"),
			said("R4.1", "direct", "Chart assessments and medications in Epic EHR"), said("R5.1", "none"),
			said("R5.2", "direct", "Coordinate discharge plans with physicians, case managers, and families"),
			said("R6.1", "direct", "Provide bedside care for 5 to 6 telemetry patients"),
			said("R7.1", "direct", "Precept new graduate nurses during their first 12 weeks"),
			said("R8.1", "direct", "BSN Nursing") };

	private static final Said[] JAKE_NURSING = { said("R1.1", "none"), said("R2.1", "none"), said("R3.1", "unclear"),
			said("R3.2", "none"), said("R4.1", "none"), said("R5.1", "none"), said("R5.2", "none"), said("R6.1", "none"),
			said("R7.1", "none"), said("R8.1", "none") };

	private static final Said[] JAKE_DATA = {
			said("R1.1", "transferable", "Wrote a Python script that reconciles QuickBooks balance-sheet and bank CSVs"),
			said("R1.2", "none"), said("R2.1", "none"), said("R2.2", "none"),
			said("R3.1", "direct", "Use SQL against PostgreSQL"), said("R3.2", "none"), said("R4.1", "none"),
			said("R4.2", "transferable", "reconciles QuickBooks balance-sheet and bank CSVs"), said("R5.1", "none"),
			said("R5.2", "none"), said("R6.1", "none"),
			said("R6.2", "transferable", "investigate data issues, generate reports, and verify fixes"),
			said("R7.1", "none") };

	@Test
	void scoresLandInBandsSetByMeaningAndInASensibleOrder() {
		MatchResult jakeFullStack = run(jake(), fullStack(), JAKE_FULL_STACK);
		MatchResult jakeCrossingHurdles = run(jake(), CrossingHurdlesRegressionTest.posting(),
				CrossingHurdlesRegressionTest.modelAnswer(said("R10.3", "none")));
		MatchResult jakeData = run(jake(), dataEngineering(), JAKE_DATA);
		MatchResult jakeNursing = run(jake(), nursing(), JAKE_NURSING);
		MatchResult nurseNursing = run(nurse(), nursing(), NURSE_NURSING);
		MatchResult nurseFullStack = run(nurse(), fullStack(), NONE_FULL_STACK);

		report("Jake / full-stack", jakeFullStack);
		report("Jake / Crossing Hurdles", jakeCrossingHurdles);
		report("Jake / data engineering", jakeData);
		report("Jake / nursing", jakeNursing);
		report("Nurse / nursing", nurseNursing);
		report("Nurse / full-stack", nurseFullStack);

		assertThat(jakeFullStack.score().total()).isGreaterThanOrEqualTo(75);
		assertThat(nurseNursing.score().total()).isGreaterThanOrEqualTo(75);
		assertThat(jakeNursing.score().total()).isLessThanOrEqualTo(20);
		assertThat(nurseFullStack.score().total()).isLessThanOrEqualTo(20);
		assertThat(jakeData.score().total()).isStrictlyBetween(20, 75);

		for (boolean ifConfirmed : new boolean[] { false, true }) {
			assertThat(total(jakeFullStack, ifConfirmed)).isGreaterThan(total(jakeCrossingHurdles, ifConfirmed));
			assertThat(total(jakeCrossingHurdles, ifConfirmed)).isGreaterThan(total(jakeData, ifConfirmed));
			assertThat(total(jakeData, ifConfirmed)).isGreaterThan(total(jakeNursing, ifConfirmed));
			assertThat(total(nurseNursing, ifConfirmed)).isGreaterThan(total(nurseFullStack, ifConfirmed));
		}
	}

	@Test
	void aListedTechnologyTheModelMissedIsFoundInTheirOwnLines() {
		MatchResult result = run(jake(), fullStack(), JAKE_FULL_STACK);

		PartMatch angular = part(find(result, "Experience with Angular"), "Angular");
		assertThat(angular.status()).isEqualTo("unconfirmed");
		assertThat(angular.reason()).isEqualTo("Named in your own lines.");
		assertThat(angular.evidence()).extracting(EvidenceCite::label).containsExactly("NanoHub");
		assertThat(find(result, "AWS experience").status()).isEqualTo("not_shown");
	}

	@Test
	void yearsAndLicensesWaitForTheirAnswerInsteadOfCountingAsGaps() {
		MatchResult result = run(nurse(), nursing(), NURSE_NURSING);

		assertThat(find(result, "Active California RN license").status()).isEqualTo("unclear");
		assertThat(find(result, "Current BLS certification").status()).isEqualTo("unclear");
		assertThat(find(result, "2+ years of acute care").status()).isEqualTo("direct");
		assertThat(find(result, "2+ years of acute care").canMarkMet()).isTrue();
		assertThat(result.score().toAnswer()).isEqualTo(3);
	}

	@Test
	void theSameEvidenceAppliesOnlyToPostingsItFits() {
		MatchResult data = run(jake(), dataEngineering(), JAKE_DATA);

		RequirementMatch pipelines = find(data, "Build batch and streaming data pipelines");
		assertThat(part(pipelines, "batch data pipelines").status()).isEqualTo("transferable");
		assertThat(pipelines.credit()).isCloseTo(0.25, within(1e-9));
		assertThat(find(data, "Hands-on with Spark or Kafka").status()).isEqualTo("not_shown");
		assertThat(part(find(data, "Python for data processing"), "Python").status()).isEqualTo("direct");
	}

	private static int total(MatchResult result, boolean ifConfirmed) {
		Integer total = ifConfirmed ? result.score().totalIfConfirmed() : result.score().total();
		return total == null ? 0 : total;
	}

	private static void report(String name, MatchResult result) {
		MatchScore score = result.score();
		System.out.printf(Locale.ROOT, "calibration | %-24s | now %s | if confirmed %s | %s%n", name, score.total(),
				score.totalIfConfirmed(), score.formula());
	}

}
