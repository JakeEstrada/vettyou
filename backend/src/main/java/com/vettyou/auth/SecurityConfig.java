package com.vettyou.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.vettyou.common.ApiErrorHttpWriter;

@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ApiErrorHttpWriter errors) throws Exception {
		http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
			.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
			.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/health", "/actuator/health", "/actuator/health/**")
				.permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/logout")
				.permitAll()
				.anyRequest()
				.authenticated())
			.exceptionHandling(handling -> handling
				.authenticationEntryPoint((request, response, authException) -> errors.write(response, 401,
						"UNAUTHENTICATED", "Authentication is required"))
				.accessDeniedHandler((request, response, accessDeniedException) -> errors.write(response, 403,
						"FORBIDDEN", "You are not allowed to perform this action")));
		return http.build();
	}

	/**
	 * Writes the CSRF cookie on the first response. Angular reads {@code XSRF-TOKEN} and sends it back as
	 * {@code X-XSRF-TOKEN}.
	 */
	static final class CsrfCookieFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws ServletException, IOException {
			CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
			if (token != null) {
				token.getToken();
			}
			chain.doFilter(request, response);
		}

	}

}
