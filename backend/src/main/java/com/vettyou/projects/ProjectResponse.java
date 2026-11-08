package com.vettyou.projects;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.vettyou.evidence.ClaimResponse;

public record ProjectResponse(
		UUID id,
		String name,
		String summary,
		String repoUrl,
		String liveUrl,
		List<Source> sources,
		List<ClaimResponse> claims) {

	public record Source(UUID id, int versionNumber, String fileName, String body, Instant createdAt) {

		static Source from(ProjectSourceVersion version) {
			return new Source(version.getId(), version.getVersionNumber(), version.getFileName(), version.getBody(),
					version.getCreatedAt());
		}

	}

}
