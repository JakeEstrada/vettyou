package com.vettyou.matching;

import java.util.List;

/** {@code evidenceChanged} is true when the saved lines differ from the ones the stored judgments saw. */
public record MatchResult(List<RequirementMatch> requirements, MatchScore score, boolean evidenceChanged) {
}
