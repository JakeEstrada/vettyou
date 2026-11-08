package com.vettyou.applications;

import java.util.List;

public record HistoryImportResponse(int imported, int skipped, List<ApplicationResponse> applications) {
}
