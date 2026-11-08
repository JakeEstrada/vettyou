package com.vettyou.mail;

class MailAuthException extends RuntimeException {

	MailAuthException() {
		super("The mailbox username or password was not accepted.");
	}

}
