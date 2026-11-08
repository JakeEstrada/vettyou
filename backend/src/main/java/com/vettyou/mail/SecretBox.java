package com.vettyou.mail;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class SecretBox {

	private static final int IV_BYTES = 12;

	private static final int TAG_BITS = 128;

	private final SecretKey key;

	SecretBox(@Value("${vettyou.mail.encryption-key:}") String configured) {
		String value = configured == null ? "" : configured.strip();
		this.key = value.isBlank() ? null : new SecretKeySpec(sha256(value), "AES");
	}

	boolean ready() {
		return key != null;
	}

	String seal(String plaintext) {
		requireKey();
		byte[] iv = new byte[IV_BYTES];
		new SecureRandom().nextBytes(iv);
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
			byte[] packed = new byte[iv.length + ciphertext.length];
			System.arraycopy(iv, 0, packed, 0, iv.length);
			System.arraycopy(ciphertext, 0, packed, iv.length, ciphertext.length);
			return Base64.getEncoder().encodeToString(packed);
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Mailbox secret could not be sealed", ex);
		}
	}

	String open(String packed) {
		requireKey();
		byte[] raw = Base64.getDecoder().decode(packed);
		if (raw.length <= IV_BYTES) {
			throw new IllegalStateException("Mailbox secret is truncated");
		}
		byte[] iv = new byte[IV_BYTES];
		byte[] ciphertext = new byte[raw.length - IV_BYTES];
		System.arraycopy(raw, 0, iv, 0, IV_BYTES);
		System.arraycopy(raw, IV_BYTES, ciphertext, 0, ciphertext.length);
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Mailbox secret could not be opened", ex);
		}
	}

	private void requireKey() {
		if (key == null) {
			throw new MailNotReadyException();
		}
	}

	private static byte[] sha256(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("SHA-256 is required", ex);
		}
	}

}
