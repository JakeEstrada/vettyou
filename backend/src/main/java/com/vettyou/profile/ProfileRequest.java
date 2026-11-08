package com.vettyou.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code email} is the address the person applies with, printed on resumes. It can differ from the login. */
public record ProfileRequest(
		@NotBlank(message = "Name is required") @Size(max = 120) String displayName,
		@Email(message = "Enter an email address like name@example.com") @Size(max = 200) String email,
		@Size(max = 40) String phone,
		@Size(max = 120) String location,
		@Size(max = 200) String website,
		@Size(max = 200) String linkedin) {
}
