package com.vettyou.auth;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountUserDetailsService implements UserDetailsService {

	private final UserRepository users;

	public AccountUserDetailsService(UserRepository users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String email) {
		UserAccount account = users.findByEmail(AuthService.normalize(email))
			.orElseThrow(() -> new UsernameNotFoundException("Unknown email"));
		return User.withUsername(account.getEmail()).password(account.getPasswordHash()).roles("USER").build();
	}

}
