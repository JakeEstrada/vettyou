package com.vettyou.projects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReadmeUploadRequest(
		@NotBlank(message = "File name is required") @Size(max = 200) String fileName,
		@NotBlank(message = "README text is required") @Size(max = 200_000, message = "README must be 200,000 characters or fewer") String body) {
}
