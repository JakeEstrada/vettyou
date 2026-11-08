package com.vettyou.mail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MailConnectionRequest(
		@NotBlank(message = "Email is required") @Email(message = "Enter a valid email") @Size(max = 320) String email,
		@NotBlank(message = "Password is required") @Size(max = 256) String password,
		@Size(max = 255) String host,
		Integer port) {
}
