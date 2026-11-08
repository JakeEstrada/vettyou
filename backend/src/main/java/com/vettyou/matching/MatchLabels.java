package com.vettyou.matching;

/** Plain words for match statuses, shared by everything that explains a match. */
public final class MatchLabels {

	private MatchLabels() {
	}

	public static String requirement(String status) {
		return switch (status) {
			case "direct" -> "Direct evidence";
			case "partial" -> "Transferable or partial evidence";
			case "unconfirmed" -> "Needs your confirmation";
			case "unclear" -> "Needs clarification";
			case "hidden" -> "In your evidence, not on this resume";
			case "gap" -> "Confirmed gap";
			case "met" -> "You confirmed you meet this";
			default -> "Not evidenced in the available material";
		};
	}

	public static String part(String status) {
		return switch (status) {
			case "direct" -> "Direct";
			case "transferable" -> "Transferable";
			case "unconfirmed" -> "Not confirmed yet";
			case "unclear" -> "Needs clarification";
			case "changed" -> "Line changed";
			case "hidden" -> "Not on this resume";
			default -> "Not shown";
		};
	}

	public static String group(String group) {
		return switch (group) {
			case "responsibility" -> "Responsibilities";
			case "preferred" -> "Nice to have";
			default -> "Required qualifications";
		};
	}

}
