package com.vettyou.matching;

import static com.vettyou.matching.MatchFixtures.confirmed;
import static com.vettyou.matching.MatchFixtures.find;
import static com.vettyou.matching.MatchFixtures.jake;
import static com.vettyou.matching.MatchFixtures.judged;
import static com.vettyou.matching.MatchFixtures.score;
import static com.vettyou.matching.MatchFixtures.stored;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class MetricsRuleTest {

	@Test
	void aMetricsFeatureIsNotUsingMetrics() {
		assertThat(MetricsRule.shown("Built the Liminnality acquisition funnel with admin metrics and site engagement "
				+ "analytics")).isFalse();
		assertThat(MetricsRule.shown("Dashboards break income, expenses, and net into tax lines")).isFalse();
		assertThat(MetricsRule.shown("Use SQL against PostgreSQL to investigate data issues and generate reports"))
			.isFalse();

		assertThat(MetricsRule.shown("Time tracking cut a two-hour manual process to about two minutes")).isTrue();
		assertThat(MetricsRule.shown("Reduced late deliveries by 30% in one quarter")).isTrue();
		assertThat(MetricsRule.shown("Reviewed conversion metrics weekly and rewrote the onboarding emails")).isTrue();
		assertThat(MetricsRule.shown("Tracked sort accuracy for each shift and retrained the slowest lines")).isTrue();
	}

	@Test
	void storedMetricsJudgmentsNeedALineThatShowsMetricsInUse() {
		Library jake = confirmed(jake());
		RequirementInput feature = stored("qualification", "required", "all", "Use metrics to drive improvements.",
				judged(jake, "using metrics to drive improvements", "data-driven improvement", "direct",
						"admin metrics and site engagement analytics"));
		RequirementInput measured = stored("qualification", "required", "all", "Use metrics to drive improvements, too.",
				judged(jake, "using metrics to drive improvements", "data-driven improvement", "transferable",
						"cut a two-hour manual process to about two minutes"));
		RequirementInput analytics = stored("qualification", "required", "all", "Build site analytics.",
				judged(jake, "site engagement analytics", "analytics", "direct",
						"admin metrics and site engagement analytics"));

		MatchResult result = score(jake, List.of(feature, measured, analytics));

		PartMatch dropped = find(result, "Use metrics to drive improvements.").parts().getFirst();
		assertThat(dropped.status()).isEqualTo("not_shown");
		assertThat(dropped.reason()).isEqualTo(MetricsRule.NOT_MEASURED);
		assertThat(dropped.evidence()).isEmpty();
		assertThat(find(result, "Use metrics to drive improvements, too.").status()).isEqualTo("partial");
		assertThat(find(result, "Build site analytics.").status()).isEqualTo("direct");
	}

	@Test
	void onlyLinesThatShowMetricsInUseStayCited() {
		Library jake = jake();
		RequirementInput delivery = stored("responsibility", "required", "all", "Track delivery metrics.",
				judged(jake, "delivery metrics", "delivery metrics", "direct", "admin metrics and site engagement analytics",
						"cut a two-hour manual process to about two minutes"));

		RequirementMatch match = find(score(jake, List.of(delivery)), "Track delivery metrics.");

		assertThat(match.parts().getFirst().evidence()).singleElement()
			.satisfies(cite -> assertThat(cite.excerpt()).isEqualTo("cut a two-hour manual process to about two minutes"));
		assertThat(match.status()).isEqualTo("unconfirmed");
		assertThat(match.credit()).isZero();
	}

	@Test
	void unsupportedJudgmentsAreLeftAlone() {
		PartInput part = new PartInput(null, "quality and delivery metrics", "pipeline metrics", null, "", List.of());
		PartJudgment unclear = new PartJudgment("unclear", "Cannot tell.", List.of());

		assertThat(MetricsRule.check(part, unclear)).isSameAs(unclear);
	}

}
