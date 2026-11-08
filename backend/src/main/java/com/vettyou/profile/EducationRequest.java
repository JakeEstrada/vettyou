package com.vettyou.profile;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EducationRequest(
		@NotBlank(message = "School is required") @Size(max = 160) String institution,
		@Size(max = 160) String credential,
		@Size(max = 160) String fieldOfStudy,
		LocalDate startDate,
		LocalDate endDate) {
}
