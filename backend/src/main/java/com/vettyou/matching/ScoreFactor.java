package com.vettyou.matching;

/**
 * One term of the Vettyou estimate. {@code value} is 0 to 1, or null when the factor is left out (its weight then
 * drops from the divisor). {@code activeWeight} is the weight the total uses: {@code weight} for a coverage group,
 * {@code weight} x coverage for evidence strength, null when left out. The {@code IfConfirmed} fields are the same
 * factor counting unconfirmed project lines.
 */
public record ScoreFactor(String key, String label, int weight, Double activeWeight, Double activeWeightIfConfirmed,
		Double value, Double valueIfConfirmed, String how) {
}
