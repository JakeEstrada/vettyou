package com.vettyou.projects;

import jakarta.validation.constraints.Size;

public record ProjectDetailsRequest(
		@Size(max = 4000) String summary,
		@Size(max = 500) String repoUrl,
		@Size(max = 500) String liveUrl) {
}
