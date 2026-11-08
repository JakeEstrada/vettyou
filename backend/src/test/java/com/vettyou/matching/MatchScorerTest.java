package com.vettyou.matching;

import static com.vettyou.matching.MatchFixtures.LIMINAL;
import static com.vettyou.matching.MatchFixtures.LIMINNALITY;
import static com.vettyou.matching.MatchFixtures.answered;
import static com.vettyou.matching.MatchFixtures.confirmed;
import static com.vettyou.matching.MatchFixtures.factors;
import static com.vettyou.matching.MatchFixtures.feature;
import static com.vettyou.matching.MatchFixtures.jake;
import static com.vettyou.matching.MatchFixtures.judged;
import static com.vettyou.matching.MatchFixtures.score;
import static com.vettyou.matching.MatchFixtures.stored;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vettyou.evidence.ClaimView;
import com.vettyou.profile.ProfileResponse;

class MatchScorerTest {

	private final Library jake = jake();

	private RequirementInput restApis(Library library) {
		return stored("qualification", "required", "all", "Experience designing REST APIs",
				judged(library, "REST APIs", "REST APIs", "direct", "Designed REST APIs"));
	}

	@Test
	void unconfirmedProjectLinesCountOnlyInTheIfConfirmedScore() {
		RequirementInput angular = stored("qualification", "required", "all", "Experience with Angular",
				judged(jake, "Angular", "Angular", "direct", "Admin app built with Angular"));

		MatchResult result = score(jake, List.of(restApis(jake), angular));
		RequirementMatch match = result.requirements().get(1);

		assertThat(match.status()).isEqualTo("unconfirmed");
		assertThat(match.credit()).isZero();
		assertThat(match.creditIfConfirmed()).isEqualTo(1.0);
		assertThat(match.strength()).isNull();
		assertThat(match.parts().getFirst().evidence().getFirst().confirmed()).isFalse();
		assertThat(match.note()).contains("Confirm them on the project page");
		assertThat(result.score().toConfirm()).isEqualTo(1);
		assertThat(result.score().unroundedIfConfirmed()).isGreaterThan(result.score().unrounded());

		Library approved = confirmed(jake);
		MatchResult after = score(approved, List.of(restApis(approved), angular));
		assertThat(after.requirements().get(1).status()).isEqualTo("direct");
		assertThat(after.score().unrounded()).isCloseTo(result.score().unroundedIfConfirmed(), within(1e-9));
		assertThat(after.score().toConfirm()).isZero();
	}

	@Test
	void partsTextCannotSettleWaitForAnAnswer() {
		RequirementInput years = stored("qualification", "required", "all", "3+ years of React experience",
				judged(jake, "3+ years", "years of experience", "unclear"),
				judged(jake, "React", "React", "direct", "TypeScript, React, Node.js"));
		RequirementInput authorization = stored("qualification", "required", "all", "Authorized to work in the US",
				judged(jake, "authorized to work in the US", "work authorization", "unclear"));

		MatchResult open = score(jake, List.of(years, authorization));
		assertThat(open.requirements().get(0).status()).isEqualTo("direct");
		assertThat(open.requirements().get(0).canMarkMet()).isTrue();
		assertThat(open.requirements().get(0).note()).endsWith("Part of it needs your answer.");
		assertThat(open.requirements().get(1).status()).isEqualTo("unclear");
		assertThat(open.requirements().get(1).weight()).isNull();
		assertThat(factors(open.score(), false).get("required")).isEqualTo(1.0);
		assertThat(open.score().toAnswer()).isEqualTo(2);

		MatchResult gap = score(jake, List.of(years, answered(authorization, "gap")));
		assertThat(gap.requirements().get(1).status()).isEqualTo("gap");
		assertThat(gap.requirements().get(1).credit()).isZero();
		assertThat(factors(gap.score(), false).get("required")).isEqualTo(0.5);

		MatchResult met = score(jake, List.of(years, answered(authorization, "meets")));
		assertThat(met.requirements().get(1).status()).isEqualTo("met");
		assertThat(met.requirements().get(1).strength()).isNull();
		assertThat(met.score().toAnswer()).isEqualTo(1);
	}

	@Test
	void anyOneItemWaitsWhenTheOnlyOtherOptionIsUnclear() {
		RequirementInput open = stored("qualification", "required", "any", "Rust or equivalent systems experience",
				judged(jake, "Rust", "Rust", "none"),
				judged(jake, "equivalent systems experience", "systems programming", "unclear"));
		RequirementInput shown = stored("qualification", "required", "any", "MongoDB or another document store",
				judged(jake, "MongoDB", "MongoDB", "direct", "PostgreSQL and MongoDB"),
				judged(jake, "another document store", "document databases", "unclear"));

		MatchResult result = score(jake, List.of(restApis(jake), open, shown));

		assertThat(result.requirements().get(1).status()).isEqualTo("unclear");
		assertThat(result.requirements().get(1).weight()).isNull();
		assertThat(result.requirements().get(2).status()).isEqualTo("direct");
		assertThat(result.requirements().get(2).credit()).isEqualTo(1.0);
		assertThat(result.requirements().get(2).note()).startsWith("One of these is enough.");
	}

	@Test
	void editedLinesStopCounting() {
		PartInput gone = new PartInput(UUID.randomUUID(), "Kubernetes", "Kubernetes", "direct", "Stored reason.",
				List.of(new EvidenceRef("job", LIMINAL, "Built a Kubernetes operator for billing",
						"Built a Kubernetes operator for billing")));
		RequirementInput kubernetes = stored("qualification", "required", "all", "Kubernetes", gone);

		RequirementMatch match = score(jake, List.of(restApis(jake), kubernetes)).requirements().get(1);

		assertThat(match.parts().getFirst().status()).isEqualTo("changed");
		assertThat(match.status()).isEqualTo("not_shown");
		assertThat(match.note()).contains("edited or removed");
	}

	@Test
	void repeatingOneProjectDoesNotRaiseStrengthAndOtherWorkAddsLittle() {
		List<ClaimView> claims = new ArrayList<>(jake.claims());
		claims.add(feature(LIMINNALITY, "Automated stage transitions across the 12-stage pipeline", "approved"));
		claims.add(feature(LIMINNALITY, "Automated pipeline transitions trigger the next production step", "approved"));
		Library library = confirmed(MatchFixtures.withClaims(jake, claims));

		double once = automation(library, "Engineered a 12-stage sales and production pipeline");
		double sameProject = automation(library, "Engineered a 12-stage sales and production pipeline",
				"Automated stage transitions across the 12-stage pipeline",
				"Automated pipeline transitions trigger the next production step",
				"built Liminnality to replace them");
		double otherWork = automation(library, "Engineered a 12-stage sales and production pipeline",
				"Built spreadsheets, quoting tools, and other automation");

		assertThat(sameProject).isEqualTo(once);
		assertThat(otherWork).isCloseTo(once + 0.05, within(1e-9));

		MatchResult single = score(library, List.of(automationItem(library, "Engineered a 12-stage sales and production pipeline")));
		MatchResult repeated = score(library, List.of(automationItem(library, "Engineered a 12-stage sales and production pipeline",
				"Automated stage transitions across the 12-stage pipeline", "built Liminnality to replace them",
				"Built spreadsheets, quoting tools, and other automation", "Kept sort operations on schedule")));
		assertThat(repeated.score().unrounded() - single.score().unrounded()).isBetween(0.0, 2.0);
	}

	private double automation(Library library, String... excerpts) {
		return score(library, List.of(automationItem(library, excerpts))).requirements().getFirst().strength();
	}

	private RequirementInput automationItem(Library library, String... excerpts) {
		return stored("qualification", "required", "all", "Experience automating workflows",
				judged(library, "automating workflows", "workflow automation", "direct", excerpts));
	}

	@Test
	void anIdeaMissingFromTwoResponsibilitiesIsPenalizedOnce() {
		RequirementInput delivery = stored("responsibility", "required", "all", "Own delivery and client alignment",
				judged(jake, "own delivery", "project ownership", "none"),
				judged(jake, "client alignment", "client alignment", "direct", "Work directly with businesses"));
		RequirementInput leadership = stored("responsibility", "required", "all", "Provide technical leadership and documentation",
				judged(jake, "technical leadership", "project ownership", "none"),
				judged(jake, "documentation", "documentation", "direct", "write down the cause and the fix"));

		MatchResult result = score(jake, List.of(delivery, leadership));

		assertThat(factors(result.score(), false).get("responsibility")).isEqualTo(0.6667);
		assertThat(result.requirements().getFirst().parts().getFirst().sharedBy()).isEqualTo(2);
		assertThat(result.requirements().getFirst().parts().getFirst().weight()).isEqualTo(0.25);
	}

	@Test
	void contactDetailsChangeReadinessButNotFit() {
		Library withPhone = MatchFixtures.withProfile(jake,
				new ProfileResponse("Jake Estrada", "jake@example.com", "(949) 555-0100", "Lake Forest, CA", "", ""));

		MatchResult without = score(jake, List.of(restApis(jake)));
		MatchResult with = score(withPhone, List.of(restApis(withPhone)));

		assertThat(with.score().unrounded()).isEqualTo(without.score().unrounded());
		assertThat(without.score().readiness().value()).isCloseTo(5.0 / 7, within(1e-9));
		assertThat(with.score().readiness().value()).isEqualTo(1.0);
	}

	@Test
	void theFormulaShowsEveryActiveWeightAndTheDivisor() {
		MatchResult result = score(jake, List.of(restApis(jake)));
		double strength = factors(result.score(), false).get("strength");

		assertThat(result.score().formula()).isEqualTo(String.format(java.util.Locale.ROOT,
				"100 x (50 x 1.0000 + 15 x %.4f) / 65 = %.2f, rounded to %d. Evidence strength weight: 15 x 1.0000 "
						+ "coverage = 15.", strength, result.score().unrounded(),
				result.score().total()));
		assertThat(result.score().unrounded()).isCloseTo(100 * (50 + 15 * strength) / 65, within(1e-9));
	}

	@Test
	void wellWrittenLinesCannotCarryAPostingYouDoNotFit() {
		RequirementInput license = stored("qualification", "required", "all", "Active California RN license.",
				judged(jake, "Active California RN license", "nursing license", "none"));
		RequirementInput educate = stored("responsibility", "required", "all", "Educate patients and families.",
				judged(jake, "Educate patients and families", "patient education", "transferable",
						"Supervised and trained warehouse teams of up to 20"));

		MatchScore score = score(jake, List.of(license, educate)).score();
		ScoreFactor strength = score.factors().stream().filter(factor -> factor.key().equals("strength")).findFirst()
			.orElseThrow();

		assertThat(strength.value()).isGreaterThan(0.5);
		assertThat(strength.activeWeight()).isCloseTo(1.731, within(1e-9));
		assertThat(score.unrounded()).isCloseTo(100 * (7.5 + 15 * 0.1154 * strength.value()) / (65 + 15 * 0.1154),
				within(1e-6));
		assertThat(score.total()).isLessThanOrEqualTo(15);
	}

	@Test
	void nothingScorableGivesNoTotal() {
		RequirementInput authorization = stored("qualification", "required", "all", "Authorized to work in the US",
				judged(jake, "authorized to work in the US", "work authorization", "unclear"));

		MatchScore score = score(jake, List.of(authorization)).score();

		assertThat(score.total()).isNull();
		assertThat(score.formula()).isEqualTo("Nothing can be scored yet.");
	}

}
