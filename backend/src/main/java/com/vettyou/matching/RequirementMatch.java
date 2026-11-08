package com.vettyou.matching;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A job requirement next to the person's evidence.
 * <ul>
 * <li>{@code group} is {@code required}, {@code responsibility}, or {@code preferred}.</li>
 * <li>{@code status} is {@code direct}, {@code partial}, {@code unconfirmed}, {@code unclear}, {@code not_shown},
 * {@code gap} (the person said they lack it), or {@code met} (the person said they meet it). On a tailored resume it
 * can also be {@code hidden}: saved lines show it, but none of them is on the resume.</li>
 * <li>{@code weight} and {@code credit} are what the group score uses; both are null when the item is left out
 * until answered. {@code creditIfConfirmed} counts unconfirmed project lines too.</li>
 * <li>{@code strength} is the evidence strength of the lines behind the credit, null without credit.</li>
 * </ul>
 */
public record RequirementMatch(
		UUID id,
		String skill,
		String kind,
		String importance,
		String group,
		String category,
		String quote,
		String logic,
		String status,
		String note,
		String answer,
		Instant answeredAt,
		boolean canMarkMet,
		Double weight,
		Double credit,
		Double creditIfConfirmed,
		Double strength,
		List<PartMatch> parts) {
}
