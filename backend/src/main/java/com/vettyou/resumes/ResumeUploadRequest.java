package com.vettyou.resumes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResumeUploadRequest(
		@NotBlank(message = "File name is required") @Size(max = 200) String fileName,
		@NotBlank(message = "Resume text is required") @Size(max = 8_000_000) String body,
		@Size(max = 20) String encoding) {
}
