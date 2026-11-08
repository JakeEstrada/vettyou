package com.vettyou.profile;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.vettyou.auth.AuthService;
import com.vettyou.auth.UserAccount;
import com.vettyou.auth.UserRepository;

@Service
public class ProfileService {

	private final UserRepository users;

	private final ProfileRepository profiles;

	public ProfileService(UserRepository users, ProfileRepository profiles) {
		this.users = users;
		this.profiles = profiles;
	}

	@Transactional(readOnly = true)
	public ProfileResponse get(String email) {
		UserAccount account = account(email);
		return profiles.findByUserId(account.getId()).map(ProfileResponse::from).orElseGet(ProfileResponse::empty);
	}

	@Transactional(readOnly = true)
	public ProfileResponse forUser(UUID userId) {
		return profiles.findByUserId(userId).map(ProfileResponse::from).orElseGet(ProfileResponse::empty);
	}

	@Transactional
	public ProfileResponse save(String email, ProfileRequest request) {
		UserAccount account = account(email);
		Profile profile = profiles.findByUserId(account.getId()).orElseGet(() -> Profile.create(account.getId(), request));
		profile.apply(request);
		return ProfileResponse.from(profiles.save(profile));
	}

	private UserAccount account(String email) {
		return users.findByEmail(AuthService.normalize(email))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required"));
	}

}
