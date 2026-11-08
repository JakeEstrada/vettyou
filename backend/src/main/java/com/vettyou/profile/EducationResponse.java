package com.vettyou.profile;

import java.time.LocalDate;
import java.util.UUID;

public record EducationResponse(
		UUID id,
		String institution,
		String credential,
		String fieldOfStudy,
		LocalDate startDate,
		LocalDate endDate) {

	static EducationResponse from(Education education) {
		return new EducationResponse(education.getId(), education.getInstitution(), education.getCredential(),
				education.getFieldOfStudy(), education.getStartDate(), education.getEndDate());
	}

}
