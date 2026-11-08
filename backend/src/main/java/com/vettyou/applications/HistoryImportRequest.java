package com.vettyou.applications;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record HistoryImportRequest(
		@NotEmpty(message = "Add at least one job.") @Size(max = 100, message = "Import at most 100 jobs at a time.") List<HistoryJobRequest> jobs) {
}
