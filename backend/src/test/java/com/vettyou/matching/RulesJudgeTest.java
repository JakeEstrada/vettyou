package com.vettyou.matching;

import static com.vettyou.matching.MatchFixtures.DETECTOR;
import static com.vettyou.matching.MatchFixtures.item;
import static com.vettyou.matching.MatchFixtures.jake;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RulesJudgeTest {

	private final Library jake = jake();

	private final EvidenceIndex index = EvidenceIndex.of(jake);

	private final RulesJudge rules = new RulesJudge(DETECTOR);

	private PartJudgment judge(String category, String quote, String part) {
		RequirementInput requirement = item("qualification", "required", "all", category, quote, part, part);
		return rules.judge(requirement, requirement.parts().getFirst(), index, jake);
	}

	@Test
	void aListedTechnologyCountsWhereTheirLinesNameItOncePerPieceOfWork() {
		PartJudgment react = judge("framework", "React", "React");

		assertThat(react.support()).isEqualTo("direct");
		assertThat(react.evidence()).hasSize(1);
		assertThat(react.evidence().getFirst().sourceId()).isEqualTo(MatchFixtures.LIMINAL);
	}

	@Test
	void phrasesNeedTheirOwnWordsInOrderNotOneWordInCommon() {
		assertThat(judge("practice", "REST APIs", "REST APIs").support()).isEqualTo("direct");
		assertThat(judge("practice", "Leading data workflows", "data workflows").support()).isEqualTo("none");
		assertThat(judge("soft_skill", "Stakeholder management", "stakeholder management").support()).isEqualTo("none");
		assertThat(judge("practice", "Execution consistency", "execution consistency").support()).isEqualTo("none");
		assertThat(judge("framework", "Vue or React", "Vue or React").support()).isEqualTo("direct");
	}

	@Test
	void yearsWaitForAnAnswerAndCredentialsAreNeverAssumed() {
		PartJudgment years = judge("experience", "3+ years of React", "3+ years of React");
		PartJudgment certification = judge("credential", "AWS certification", "AWS certification");

		assertThat(years.support()).isEqualTo("unclear");
		assertThat(years.evidence()).isNotEmpty();
		assertThat(certification.support()).isEqualTo("none");
		assertThat(certification.reason()).startsWith("Only counts if you hold it.");
	}

	@Test
	void degreesCompareLevelAndField() {
		PartJudgment computerScience = judge("education", "Bachelor's degree in Computer Science",
				"Bachelor's degree in Computer Science");
		PartJudgment nursing = judge("education", "Bachelor's degree in Nursing", "Bachelor's degree in Nursing");
		PartJudgment masters = judge("education", "Master's degree in Data Science", "Master's degree in Data Science");

		assertThat(computerScience.support()).isEqualTo("direct");
		assertThat(computerScience.evidence()).singleElement().satisfies(evidence -> {
			assertThat(evidence.kind()).isEqualTo("school");
			assertThat(evidence.sourceId()).isEqualTo(MatchFixtures.CSUF);
		});
		assertThat(nursing.support()).isEqualTo("transferable");
		assertThat(masters.support()).isEqualTo("none");
	}

}
