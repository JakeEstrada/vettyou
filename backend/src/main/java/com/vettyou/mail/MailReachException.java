package com.vettyou.mail;

class MailReachException extends RuntimeException {

	MailReachException() {
		super("The mailbox host could not be reached.");
	}

}
