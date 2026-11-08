package com.vettyou.matching;

import java.util.List;

/** Whether the profile has what any application needs. Shown next to the score, never part of it. */
public record Readiness(double value, List<Check> checks) {

	public record Check(String label, boolean done) {
	}

}
