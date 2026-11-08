package com.vettyou.profile;

public record ProfileResponse(String displayName, String email, String phone, String location, String website,
		String linkedin) {

	static ProfileResponse empty() {
		return new ProfileResponse("", "", "", "", "", "");
	}

	static ProfileResponse from(Profile profile) {
		return new ProfileResponse(profile.getDisplayName(), profile.getEmail(), profile.getPhone(),
				profile.getLocation(), profile.getWebsite(), profile.getLinkedin());
	}

}
