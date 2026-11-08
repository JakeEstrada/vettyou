package com.vettyou.matching;

import java.util.List;

/**
 * The Vettyou estimate for one posting, from confirmed evidence only, and again as if every flagged project line were
 * confirmed. Totals are null when nothing can be scored yet. {@code formula} shows the sum with the numbers filled
 * in, so every total can be checked by hand.
 */
public record MatchScore(
		Integer total,
		Double unrounded,
		Integer totalIfConfirmed,
		Double unroundedIfConfirmed,
		List<ScoreFactor> factors,
		String formula,
		String formulaIfConfirmed,
		int toConfirm,
		int toAnswer,
		Readiness readiness,
		String label,
		String note) {
}
