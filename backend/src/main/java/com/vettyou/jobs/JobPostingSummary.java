package com.vettyou.jobs;

import java.time.Instant;
import java.util.UUID;

/** {@code id} doubles as the work item to poll while {@code status} is {@code reading} or {@code matching}. */
public record JobPostingSummary(UUID id, String title, String company, String pay, String preview, String status,
		Instant createdAt) {

	static JobPostingSummary from(JobPosting posting) {
		String company = PostingCopy.company(posting.getCompany(), posting.getBody());
		String title = PostingCopy.title(posting.getTitle(), company, posting.getBody());
		return new JobPostingSummary(posting.getId(), title, company, PostingCopy.pay(posting.getBody()),
				PostingCopy.preview(posting.getBody(), title, company), posting.getStatus(), posting.getCreatedAt());
	}

}
