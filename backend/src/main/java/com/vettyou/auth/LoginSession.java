package com.vettyou.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginSession {

	private final AuthenticationManager authenticationManager;

	private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

	public LoginSession(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	public void login(String email, String password, HttpServletRequest request, HttpServletResponse response) {
		var authentication = authenticationManager
			.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(AuthService.normalize(email), password));
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
	}

	public void logout(HttpServletRequest request, HttpServletResponse response) {
		new SecurityContextLogoutHandler().logout(request, response,
				SecurityContextHolder.getContext().getAuthentication());
	}

}
