package com.vettyou.mail;

import java.util.List;

public interface ImapGateway {

	void login(String host, int port, String username, String password);

	ImapFetch fetch(String host, int port, String username, String password, String folder, long uidValidity,
			long lastUid, int limit);

	List<String> listFolders(String host, int port, String username, String password);

}
