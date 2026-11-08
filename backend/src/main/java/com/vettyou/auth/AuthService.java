package com.vettyou.auth;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository users;

	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public AccountResponse register(RegisterRequest request) {
		String email = normalize(request.email());
		if (users.existsByEmail(email)) {
			throw new DuplicateEmailException();
		}
		UserAccount account = users.saveAndFlush(UserAccount.create(email, passwordEncoder.encode(request.password())));
		return AccountResponse.from(account);
	}

	@Transactional(readOnly = true)
	public AccountResponse current(String email) {
		return users.findByEmail(normalize(email))
			.map(AccountResponse::from)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required"));
	}

	public static String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
