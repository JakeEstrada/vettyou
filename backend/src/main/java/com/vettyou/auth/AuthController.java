package com.vettyou.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

	private final AuthService authService;

	private final LoginSession loginSession;

	public AuthController(AuthService authService, LoginSession loginSession) {
		this.authService = authService;
		this.loginSession = loginSession;
	}

	@PostMapping("/auth/register")
	public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http,
			HttpServletResponse response) {
		AccountResponse account = authService.register(request);
		loginSession.login(account.email(), request.password(), http, response);
		return ResponseEntity.status(201).body(account);
	}

	@PostMapping("/auth/login")
	public AccountResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http,
			HttpServletResponse response) {
		loginSession.login(request.email(), request.password(), http, response);
		return authService.current(request.email());
	}

	@PostMapping("/auth/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		loginSession.logout(request, response);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public AccountResponse me(@AuthenticationPrincipal UserDetails principal) {
		return authService.current(principal.getUsername());
	}

}
