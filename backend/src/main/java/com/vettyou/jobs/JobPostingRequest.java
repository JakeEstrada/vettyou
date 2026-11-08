package com.vettyou.jobs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobPostingRequest(
		@NotBlank(message = "Paste the job posting") @Size(max = 60_000, message = "That posting is too long. Keep it under 60,000 characters.") String body) {
}
