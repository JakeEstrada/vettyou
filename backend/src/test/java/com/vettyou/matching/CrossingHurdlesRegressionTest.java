package com.vettyou.matching;

import static com.vettyou.matching.MatchFixtures.find;
import static com.vettyou.matching.MatchFixtures.item;
import static com.vettyou.matching.MatchFixtures.jake;
import static com.vettyou.matching.MatchFixtures.part;
import static com.vettyou.matching.MatchFixtures.run;
import static com.vettyou.matching.MatchFixtures.said;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.vettyou.matching.MatchFixtures.Said;

/**
 * The "Strategic Project Lead, Coding/SWE" posting that scored 37/100 under the keyword matcher. The model's answer is
 * written by hand; the citation checks and the score are the real code.
 */
class CrossingHurdlesRegressionTest {

	static List<RequirementInput> posting() {
		return List.of(
				item("responsibility", "required", "all", "practice",
						"Own end-to-end delivery of coding data pipelines, ensuring quality and client alignment.",
						"end-to-end delivery", "project ownership", "coding data pipelines", "coding data pipelines",
						"quality", "quality assurance", "client alignment", "client alignment"),
				item("responsibility", "required", "all", "practice",
						"Drive automation of pipeline workflows, including task generation and reporting.",
						"automation of pipeline workflows", "workflow automation", "task generation", "task generation",
						"reporting", "reporting"),
				item("responsibility", "required", "all", "soft_skill",
						"Coordinate across experts, reviewers, and clients to maintain execution consistency.",
						"coordinate across experts, reviewers, and clients", "stakeholder coordination",
						"execution consistency", "execution consistency"),
				item("responsibility", "required", "all", "practice",
						"Define task specifications and review rubrics to establish quality standards.",
						"task specifications", "specifications", "review rubrics", "review rubrics", "quality standards",
						"quality assurance"),
				item("responsibility", "required", "all", "practice",
						"Track and optimize pipeline health across quality and delivery metrics.",
						"pipeline health", "pipeline metrics", "quality and delivery metrics", "pipeline metrics"),
				item("responsibility", "required", "all", "practice",
						"Lead continuous improvement based on client feedback and performance data.",
						"continuous improvement", "continuous improvement", "client feedback and performance data",
						"data-driven improvement"),
				item("qualification", "required", "any", "education",
						"Bachelor s degree in Computer Science, Software Engineering, or a related technical field.",
						"Bachelor s degree in Computer Science", "computer science degree", "Software Engineering",
						"software engineering degree", "related technical field", "technical degree"),
				item("qualification", "required", "all", "experience",
						"Strong understanding of software engineering and coding fundamentals.",
						"software engineering fundamentals", "software engineering", "coding fundamentals", "coding"),
				item("qualification", "required", "any", "experience",
						"Have strong relevant experience leading technical projects or data workflows.",
						"leading technical projects", "project ownership", "leading data workflows", "data workflows"),
				item("qualification", "required", "all", "soft_skill",
						"Strong analytical and problem-solving skills with experience using metrics to drive improvements.",
						"analytical skills", "analysis", "problem-solving skills", "problem solving",
						"using metrics to drive improvements", "data-driven improvement"),
				item("qualification", "required", "all", "practice",
						"Ability to translate client requirements into clear technical specifications and workflows.",
						"translate client requirements", "requirements translation", "technical specifications",
						"specifications", "workflows", "workflow design"),
				item("qualification", "required", "all", "soft_skill",
						"Strong stakeholder management and communication skills across technical and client-facing teams.",
						"stakeholder management", "stakeholder coordination", "communication skills", "communication",
						"technical and client-facing teams", "cross-team work"));
	}

	static Said[] modelAnswer(Said metrics) {
		return new Said[] { said("R1.1", "direct", "Owned systems from requirements discovery through architecture"),
				said("R1.2", "none"), said("R1.3", "transferable", "add a test so it stays fixed"),
				said("R1.4", "direct", "Work directly with businesses to understand their operations"),
				said("R2.1", "transferable", "develop custom software that automates administrative processes"),
				said("R2.2", "none"), said("R2.3", "transferable", "investigate data issues, generate reports"),
				said("R3.1", "transferable", "Point of contact for customers, vendors, office staff, shop teams, and installers"),
				said("R3.2", "none"),
				said("R4.1", "transferable", "Translated operator and office needs into product requirements"),
				said("R4.2", "none"),
				said("R4.3", "transferable", "Cover the UI with Vitest and the API with Node's test runner"),
				said("R5.1", "none"), said("R5.2", "none"),
				said("R6.1", "transferable", "Identified the scheduling and tracking bottlenecks"), said("R6.2", "none"),
				said("R7.1", "direct", "B.S. Computer Science"), said("R7.2", "none"),
				said("R7.3", "direct", "B.S. Computer Science"),
				said("R8.1", "direct", "Designed, architected, and deployed production systems"),
				said("R8.2", "direct", "Cover the UI with Vitest and the API with Node's test runner"),
				said("R9.1", "direct",
						"Owned systems from requirements discovery through architecture, deployment, troubleshooting, and production support"),
				said("R9.2", "none"), said("R10.1", "direct", "investigate data issues"),
				said("R10.2", "direct", "Diagnose production issues"), metrics,
				said("R11.1", "direct", "Translated operator and office needs into product requirements"),
				said("R11.2", "transferable", "defined the requirements"),
				said("R11.3", "direct", "Designed role-based client and admin workflows"),
				said("R12.1", "direct", "Point of contact for customers, vendors"),
				said("R12.2", "direct", "Work directly with businesses"),
				said("R12.3", "direct", "Work directly with businesses to understand their operations") };
	}

	private final MatchResult result = run(jake(), posting(), modelAnswer(said("R10.3", "none")));

	@Test
	void deployedSoftwareWorkSupportsCodingFundamentals() {
		RequirementMatch fundamentals = find(result, "Strong understanding of software engineering");

		assertThat(fundamentals.status()).isEqualTo("direct");
		assertThat(fundamentals.credit()).isEqualTo(1.0);
		assertThat(fundamentals.parts()).allSatisfy(part -> {
			assertThat(part.status()).isEqualTo("direct");
			assertThat(part.evidence()).allSatisfy(cite -> {
				assertThat(cite.kind()).isEqualTo("job");
				assertThat(cite.confirmed()).isTrue();
			});
		});
	}

	@Test
	void technicalProjectOwnershipSatisfiesTheOrRequirement() {
		RequirementMatch leading = find(result, "Have strong relevant experience leading");

		assertThat(leading.logic()).isEqualTo("any");
		assertThat(leading.status()).isEqualTo("direct");
		assertThat(leading.credit()).isEqualTo(1.0);
		assertThat(part(leading, "leading data workflows").status()).isEqualTo("not_shown");
		assertThat(leading.note()).startsWith("One of these is enough.");
	}

	@Test
	void businessAutomationIsTransferableNotCodingDataPipelineExperience() {
		RequirementMatch automation = find(result, "Drive automation of pipeline workflows");

		assertThat(part(automation, "automation of pipeline workflows").status()).isEqualTo("transferable");
		assertThat(part(automation, "task generation").status()).isEqualTo("not_shown");
		assertThat(automation.status()).isEqualTo("partial");
		assertThat(automation.credit()).isCloseTo((0.5 + 0 + 0.5) / 3, within(1e-9));
		assertThat(part(find(result, "Own end-to-end delivery"), "coding data pipelines").status()).isEqualTo("not_shown");
	}

	@Test
	void metricsStayUnsupportedWithoutRealEvidenceEvenWhenTheModelClaimsThem() {
		RequirementMatch analytical = find(result, "Strong analytical and problem-solving");
		assertThat(part(analytical, "using metrics to drive improvements").status()).isEqualTo("not_shown");
		assertThat(analytical.credit()).isCloseTo(2.0 / 3, within(1e-9));
		assertThat(find(result, "Track and optimize pipeline health").status()).isEqualTo("not_shown");

		Said invented = MatchFixtures.saidOn("R10.3", "direct", "Built the Liminnality acquisition funnel",
				"Improved pipeline throughput by 40% using delivery metrics");
		MatchResult claimed = run(jake(), posting(), modelAnswer(invented));
		PartMatch metrics = part(find(claimed, "Strong analytical and problem-solving"), "using metrics to drive improvements");
		assertThat(metrics.status()).isEqualTo("not_shown");
		assertThat(metrics.reason()).isEqualTo(EvidenceMatcher.NO_QUOTE);
		assertThat(claimed.score().unrounded()).isEqualTo(result.score().unrounded());
	}

	@Test
	void aMetricsFeatureTheModelCallsDirectIsNotUsingMetrics() {
		MatchResult claimed = run(jake(), posting(), with(modelAnswer(said("R10.3", "none")),
				said("R10.3", "direct", "admin metrics and site engagement analytics"),
				said("R5.2", "direct", "admin metrics and site engagement analytics")));
		PartMatch metrics = part(find(claimed, "Strong analytical and problem-solving"),
				"using metrics to drive improvements");

		assertThat(metrics.status()).isEqualTo("not_shown");
		assertThat(metrics.reason()).isEqualTo(MetricsRule.NOT_MEASURED);
		assertThat(part(find(claimed, "Track and optimize pipeline health"), "quality and delivery metrics").status())
			.isEqualTo("not_shown");
		assertThat(claimed.score().unrounded()).isEqualTo(result.score().unrounded());
	}

	@Test
	void aMeasuredResultCanSupportMetricsOnceConfirmed() {
		MatchResult shown = run(jake(), posting(),
				modelAnswer(said("R10.3", "transferable", "cut a two-hour manual process to about two minutes")));
		PartMatch metrics = part(find(shown, "Strong analytical and problem-solving"),
				"using metrics to drive improvements");

		assertThat(metrics.status()).isEqualTo("unconfirmed");
		assertThat(shown.score().unrounded()).isEqualTo(result.score().unrounded());
		assertThat(shown.score().unroundedIfConfirmed()).isGreaterThan(result.score().unroundedIfConfirmed());
	}

	@Test
	void legitimateGapsStayGaps() {
		assertThat(part(find(result, "Define task specifications"), "review rubrics").status()).isEqualTo("not_shown");
		assertThat(part(find(result, "Coordinate across experts"), "execution consistency").status())
			.isEqualTo("not_shown");
		assertThat(find(result, "Define task specifications").status()).isEqualTo("partial");
		assertThat(result.score().factors().stream().filter(factor -> factor.key().equals("responsibility")).findFirst()
			.orElseThrow().value()).isLessThan(0.5);
	}

	@Test
	void responsibilitiesAreTheirOwnLighterGroup() {
		Map<String, Double> factors = MatchFixtures.factors(result.score(), false);

		assertThat(result.requirements().stream().filter(match -> match.group().equals("responsibility"))).hasSize(6);
		assertThat(result.requirements().stream().filter(match -> match.group().equals("required"))).hasSize(6);
		assertThat(result.score().factors()).extracting(ScoreFactor::key, ScoreFactor::weight)
			.containsExactly(org.assertj.core.groups.Tuple.tuple("required", 50),
					org.assertj.core.groups.Tuple.tuple("responsibility", 15),
					org.assertj.core.groups.Tuple.tuple("preferred", 15),
					org.assertj.core.groups.Tuple.tuple("strength", 15),
					org.assertj.core.groups.Tuple.tuple("readability", 15));
		assertThat(factors.get("required")).isGreaterThan(factors.get("responsibility"));
	}

	@Test
	void anIdeaRepeatedAcrossResponsibilitiesCountsOnce() {
		PartMatch ownQuality = part(find(result, "Own end-to-end delivery"), "quality");
		PartMatch rubricQuality = part(find(result, "Define task specifications"), "quality standards");

		assertThat(ownQuality.sharedBy()).isEqualTo(2);
		assertThat(rubricQuality.sharedBy()).isEqualTo(2);
		assertThat(ownQuality.weight() + rubricQuality.weight()).isCloseTo(1.0 / 3, within(1e-9));
	}

	@Test
	void missingOptionalCategoriesDoNotLowerTheScore() {
		Map<String, Double> factors = MatchFixtures.factors(result.score(), false);

		assertThat(factors.get("preferred")).isNull();
		assertThat(factors.get("readability")).isNull();
		double strengthWeight = result.score().factors().stream().filter(factor -> factor.key().equals("strength"))
			.findFirst().orElseThrow().activeWeight();
		assertThat(result.score().formula()).contains(") / " + BigDecimal.valueOf(50 + 15)
			.add(BigDecimal.valueOf(strengthWeight))
			.stripTrailingZeros()
			.toPlainString() + " = ");
	}

	@Test
	void theShownFormulaGivesTheShownScoreByHand() {
		for (String formula : List.of(result.score().formula(), result.score().formulaIfConfirmed())) {
			Matcher shown = Pattern.compile("100 x \\((.+)\\) / ([\\d.]+) = ([\\d.]+), rounded to (\\d+)\\. Evidence "
					+ "strength weight: 15 x ([\\d.]+) coverage = ([\\d.]+)\\.").matcher(formula);
			assertThat(shown.matches()).as(formula).isTrue();
			List<String[]> terms = java.util.Arrays.stream(shown.group(1).split(" \\+ "))
				.map(term -> term.split(" x "))
				.toList();
			BigDecimal weighted = BigDecimal.ZERO;
			for (String[] sides : terms) {
				weighted = weighted.add(new BigDecimal(sides[0]).multiply(new BigDecimal(sides[1])));
			}
			BigDecimal total = weighted.multiply(BigDecimal.valueOf(100))
				.divide(new BigDecimal(shown.group(2)), 10, RoundingMode.HALF_UP);

			assertThat(total.setScale(2, RoundingMode.HALF_UP).toPlainString()).as(formula).isEqualTo(shown.group(3));
			assertThat(total.setScale(0, RoundingMode.HALF_UP).toPlainString()).as(formula).isEqualTo(shown.group(4));

			List<String[]> coverage = terms.subList(0, terms.size() - 1);
			BigDecimal covered = BigDecimal.ZERO;
			BigDecimal coverageWeights = BigDecimal.ZERO;
			for (String[] sides : coverage) {
				covered = covered.add(new BigDecimal(sides[0]).multiply(new BigDecimal(sides[1])));
				coverageWeights = coverageWeights.add(new BigDecimal(sides[0]));
			}
			BigDecimal share = covered.divide(coverageWeights, 4, RoundingMode.HALF_UP);
			assertThat(share.toPlainString()).as(formula).isEqualTo(shown.group(5));
			assertThat(share.multiply(BigDecimal.valueOf(15)).compareTo(new BigDecimal(shown.group(6)))).as(formula)
				.isZero();
			assertThat(terms.getLast()[0]).as(formula).isEqualTo(shown.group(6));
		}
		assertThat(result.score().formula()).contains("rounded to " + result.score().total() + ".");
		assertThat(result.score().formulaIfConfirmed()).contains("rounded to " + result.score().totalIfConfirmed() + ".");
	}

	@Test
	void everyNumberCanBeRecomputedFromTheOnesNextToIt() {
		MatchScore score = result.score();
		double weighted = 0;
		double weights = 0;
		for (ScoreFactor factor : score.factors()) {
			if (factor.value() != null) {
				weighted += factor.activeWeight() * factor.value();
				weights += factor.activeWeight();
			}
		}
		assertThat(score.unrounded()).isCloseTo(100 * weighted / weights, within(1e-9));
		assertThat(score.total()).isEqualTo((int) Math.round(score.unrounded()));

		for (String group : List.of("required", "responsibility")) {
			List<RequirementMatch> items = result.requirements().stream()
				.filter(match -> match.group().equals(group) && match.weight() != null)
				.toList();
			double groupWeight = items.stream().mapToDouble(RequirementMatch::weight).sum();
			double groupValue = items.stream().mapToDouble(match -> match.weight() * match.credit()).sum() / groupWeight;
			assertThat(MatchFixtures.factors(score, false).get(group)).isCloseTo(groupValue, within(5e-5));
		}
		for (RequirementMatch match : result.requirements()) {
			if (!"all".equals(match.logic()) || match.weight() == null) {
				continue;
			}
			double credit = match.parts().stream().filter(part -> part.weight() != null)
				.mapToDouble(part -> part.weight() * value(part.status())).sum() / match.weight();
			assertThat(match.credit()).as(match.quote()).isCloseTo(credit, within(1e-9));
		}
		double strength = result.requirements().stream().filter(match -> match.strength() != null)
			.mapToDouble(RequirementMatch::strength).average().orElseThrow();
		assertThat(MatchFixtures.factors(score, false).get("strength")).isCloseTo(strength, within(5e-5));
	}

	@Test
	void missingItemsAreNotPenalizedAgainInEvidenceStrength() {
		RequirementMatch health = find(result, "Track and optimize pipeline health");

		assertThat(health.credit()).isZero();
		assertThat(health.strength()).isNull();
		assertThat(find(result, "Bachelor s degree").strength()).isNull();
	}

	@Test
	void contactDetailsAreReadinessNotFit() {
		assertThat(result.score().readiness().checks()).extracting(Readiness.Check::label, Readiness.Check::done)
			.contains(org.assertj.core.groups.Tuple.tuple("Phone", false));
		assertThat(result.score().factors()).extracting(ScoreFactor::key).doesNotContain("completeness");
		assertThat(result.score().label()).isEqualTo("Vettyou estimate");
		assertThat(result.score().note()).contains("not an employer's ATS score");
	}

	private static Said[] with(Said[] answer, Said... replacements) {
		Map<String, Said> byPart = new LinkedHashMap<>();
		for (Said said : answer) {
			byPart.put(said.part(), said);
		}
		for (Said said : replacements) {
			byPart.put(said.part(), said);
		}
		return byPart.values().toArray(Said[]::new);
	}

	private static double value(String status) {
		return switch (status) {
			case "direct" -> 1;
			case "transferable" -> 0.5;
			default -> 0;
		};
	}

}
