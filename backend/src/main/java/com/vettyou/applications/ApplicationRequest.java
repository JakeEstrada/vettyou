package com.vettyou.applications;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationRequest(
		@NotBlank(message = "Title is required") @Size(max = 200) String title,
		@Size(max = 160) String company,
		@Size(max = 120) String location,
		@Size(max = 80) String pay,
		@Size(max = 32) String status,
		UUID postingId,
		Instant appliedAt,
		@Size(max = 4000) String notes) {
}
