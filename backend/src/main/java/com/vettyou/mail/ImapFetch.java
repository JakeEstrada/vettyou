package com.vettyou.mail;

import java.util.List;

public record ImapFetch(long uidValidity, long lastUid, List<ImapMessage> messages) {

	static ImapFetch empty() {
		return new ImapFetch(0, 0, List.of());
	}

}
