package com.vettyou.profile;

import java.time.LocalDate;
import java.util.UUID;

public record ExperienceSummary(
		UUID id,
		String employer,
		String title,
		LocalDate startDate,
		LocalDate endDate,
		boolean current) {

	static ExperienceSummary from(WorkExperience experience) {
		return new ExperienceSummary(experience.getId(), experience.getEmployer(), experience.getTitle(),
				experience.getStartDate(), experience.getEndDate(), experience.isCurrentRole());
	}

}
