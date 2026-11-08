package com.vettyou.projects;

import java.util.UUID;

public record ProjectSummary(UUID id, String name, String summary) {

	static ProjectSummary from(Project project) {
		return new ProjectSummary(project.getId(), project.getName(), project.getSummary());
	}

}
