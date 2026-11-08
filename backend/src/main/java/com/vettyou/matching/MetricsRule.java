package com.vettyou.matching;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Using metrics needs real evidence (ADR 0004): each cited line must state a measured result or say the person used
 * metrics, or it is dropped. A metrics page or dashboard the person built shows the feature, not its use.
 */
final class MetricsRule {

	static final String NOT_MEASURED = "Counts only when a line of yours states a measured result or says you used "
			+ "metrics. Building a metrics feature is not enough.";

	private static final Pattern ASKS = Pattern
		.compile("(?i)\\b(?:metrics?|kpis?|measur\\w*|data[- ]driven|performance data)\\b");

	private static final Pattern USED = Pattern.compile("(?i)\\b(?:track(?:ed|ing|s)?|monitor(?:ed|ing|s)?|"
			+ "measur(?:e|ed|es|ing)|analy[sz](?:e|ed|es|ing)|review(?:ed|ing|s)?|defin(?:e|ed|es|ing)|"
			+ "optimiz(?:e|ed|es|ing)|us(?:e|ed|es|ing))\\s+(?:[\\w-]+\\s+){0,4}?(?:metrics?|kpis?|analytics|numbers|"
			+ "conversion|throughput|latency|accuracy|error rates?)\\b");

	private MetricsRule() {
	}

	static PartJudgment check(PartInput part, PartJudgment judgment) {
		boolean supported = "direct".equals(judgment.support()) || "transferable".equals(judgment.support());
		if (!supported || !ASKS.matcher(part.text() + " " + part.concept()).find()) {
			return judgment;
		}
		List<EvidenceRef> shown = judgment.evidence().stream().filter(ref -> shown(ref.line())).toList();
		if (shown.isEmpty()) {
			return new PartJudgment("none", NOT_MEASURED, List.of());
		}
		return shown.size() == judgment.evidence().size() ? judgment
				: new PartJudgment(judgment.support(), judgment.reason(), shown);
	}

	static boolean shown(String line) {
		return EvidenceQuality.measured(line) || USED.matcher(line).find();
	}

}
