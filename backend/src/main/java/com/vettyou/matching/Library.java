package com.vettyou.matching;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vettyou.evidence.ClaimView;
import com.vettyou.profile.EducationResponse;
import com.vettyou.profile.ExperienceText;
import com.vettyou.profile.ProfileResponse;

/** Everything one person has told us, loaded once per request. */
record Library(
		List<ClaimView> claims,
		Map<UUID, String> projectNames,
		List<ExperienceText> jobs,
		List<EducationResponse> schools,
		ProfileResponse profile) {

	String projectName(UUID projectId) {
		return projectNames.getOrDefault(projectId, "Project");
	}

}
