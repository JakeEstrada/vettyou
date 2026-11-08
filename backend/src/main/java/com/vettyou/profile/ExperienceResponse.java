package com.vettyou.profile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExperienceResponse(
		UUID id,
		String employer,
		String title,
		String location,
		LocalDate startDate,
		LocalDate endDate,
		boolean current,
		String body,
		List<VersionResponse> versions) {

	public record VersionResponse(int number, String body, Instant createdAt) {
	}

	static ExperienceResponse from(WorkExperience experience, List<SourceVersion> versions) {
		List<VersionResponse> history = versions.stream()
			.map(version -> new VersionResponse(version.getVersionNumber(), version.getBody(), version.getCreatedAt()))
			.toList();
		String latest = history.isEmpty() ? "" : history.get(history.size() - 1).body();
		return new ExperienceResponse(experience.getId(), experience.getEmployer(), experience.getTitle(),
				experience.getLocation(), experience.getStartDate(), experience.getEndDate(), experience.isCurrentRole(),
				latest, history);
	}

}
