package com.vettyou.profile;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExperienceRequest(
		@NotBlank(message = "Employer is required") @Size(max = 160) String employer,
		@NotBlank(message = "Job title is required") @Size(max = 160) String title,
		@Size(max = 120) String location,
		LocalDate startDate,
		LocalDate endDate,
		boolean current,
		@NotBlank(message = "Write what you did") @Size(max = 20000) String body) {
}
