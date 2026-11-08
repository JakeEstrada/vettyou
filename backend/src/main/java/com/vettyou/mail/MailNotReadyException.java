package com.vettyou.mail;

public class MailNotReadyException extends RuntimeException {

	MailNotReadyException() {
		super("Mailbox encryption is not configured.");
	}

}
