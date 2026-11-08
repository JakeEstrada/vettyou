package com.vettyou.resumes;

import java.util.List;
import java.util.UUID;

import com.vettyou.profile.ProfileResponse;

public record ResumeImportResponse(
		ProfileResponse profile,
		List<Added> jobs,
		List<Added> projects,
		List<Added> schools,
		List<String> skipped) {

	public record Added(UUID id, String label) {
	}

}
