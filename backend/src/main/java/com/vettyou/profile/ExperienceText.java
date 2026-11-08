package com.vettyou.profile;

import java.time.LocalDate;
import java.util.UUID;

/** A job with the latest saved wording, for matching and resumes. */
public record ExperienceText(
		UUID id,
		String employer,
		String title,
		String location,
		LocalDate startDate,
		LocalDate endDate,
		boolean currentRole,
		String body) {

	public String label() {
		return title + " at " + employer;
	}

	/** "2023-05 to present", "2019-01 to 2021-06", or empty when no dates are saved. */
	public String dates() {
		if (startDate == null) {
			return "";
		}
		String end = currentRole ? "present" : endDate == null ? "" : month(endDate);
		return end.isEmpty() ? month(startDate) : month(startDate) + " to " + end;
	}

	private static String month(LocalDate date) {
		return String.format("%04d-%02d", date.getYear(), date.getMonthValue());
	}

}
