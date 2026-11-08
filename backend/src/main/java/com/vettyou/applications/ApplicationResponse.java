package com.vettyou.applications;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApplicationResponse(
		UUID id,
		UUID postingId,
		String title,
		String company,
		String location,
		String pay,
		String status,
		Instant appliedAt,
		String notes,
		Instant createdAt,
		Integer estimate,
		List<ApplicationEventResponse> events) {

	static ApplicationResponse from(Application application, Integer estimate, List<ApplicationStatusEvent> events) {
		return new ApplicationResponse(application.getId(), application.getPostingId(), application.getTitle(),
				application.getCompany(), application.getLocation(), application.getPay(), application.getStatus(),
				application.getAppliedAt(), application.getNotes(), application.getCreatedAt(), estimate,
				events.stream().map(ApplicationEventResponse::from).toList());
	}

}
