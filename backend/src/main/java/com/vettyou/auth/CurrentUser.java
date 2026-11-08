package com.vettyou.auth;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUser {

	private final UserRepository users;

	public CurrentUser(UserRepository users) {
		this.users = users;
	}

	public UUID id(UserDetails principal) {
		return users.findByEmail(AuthService.normalize(principal.getUsername()))
			.map(UserAccount::getId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required"));
	}

}
