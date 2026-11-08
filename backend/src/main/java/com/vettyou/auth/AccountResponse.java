package com.vettyou.auth;

import java.util.UUID;

public record AccountResponse(UUID id, String email) {

	static AccountResponse from(UserAccount account) {
		return new AccountResponse(account.getId(), account.getEmail());
	}

}
