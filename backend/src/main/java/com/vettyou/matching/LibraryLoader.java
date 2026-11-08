package com.vettyou.matching;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.vettyou.evidence.EvidenceService;
import com.vettyou.profile.EducationService;
import com.vettyou.profile.ExperienceService;
import com.vettyou.profile.ProfileService;
import com.vettyou.projects.ProjectService;
import com.vettyou.projects.ProjectSummary;

@Component
class LibraryLoader {

	private final EvidenceService evidence;

	private final ProjectService projects;

	private final ExperienceService experiences;

	private final EducationService educations;

	private final ProfileService profiles;

	LibraryLoader(EvidenceService evidence, ProjectService projects, ExperienceService experiences,
			EducationService educations, ProfileService profiles) {
		this.evidence = evidence;
		this.projects = projects;
		this.experiences = experiences;
		this.educations = educations;
		this.profiles = profiles;
	}

	Library load(UUID userId) {
		Map<UUID, String> names = projects.list(userId).stream()
			.collect(Collectors.toMap(ProjectSummary::id, ProjectSummary::name));
		return new Library(evidence.claimsFor(userId), names, experiences.texts(userId), educations.list(userId),
				profiles.forUser(userId));
	}

}
