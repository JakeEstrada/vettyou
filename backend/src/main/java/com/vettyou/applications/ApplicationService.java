package com.vettyou.applications;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vettyou.common.InvalidFieldsException;
import com.vettyou.common.NotFoundException;
import com.vettyou.jobs.JobPosting;
import com.vettyou.jobs.JobPostingRepository;
import com.vettyou.resumes.TailoredResumeRepository;
import com.vettyou.resumes.TailoredResumeSummary;

@Service
public class ApplicationService {

	private final ApplicationRepository applications;

	private final ApplicationStatusEventRepository events;

	private final JobPostingRepository postings;

	private final TailoredResumeRepository resumes;

	public ApplicationService(ApplicationRepository applications, ApplicationStatusEventRepository events,
			JobPostingRepository postings, TailoredResumeRepository resumes) {
		this.applications = applications;
		this.events = events;
		this.postings = postings;
		this.resumes = resumes;
	}

	@Transactional(readOnly = true)
	public List<ApplicationResponse> list(UUID userId) {
		return applications.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::response).toList();
	}

	@Transactional
	public HistoryImportResponse importHistory(UUID userId, HistoryImportRequest request) {
		List<HistoryJobRequest> jobs = request.jobs();
		Set<String> seen = new HashSet<>();
		for (Application existing : applications.findByUserIdOrderByCreatedAtDesc(userId)) {
			seen.add(HistoryPosting.key(existing.getTitle(), existing.getCompany()));
		}
		for (JobPosting existing : postings.findByUserIdOrderByCreatedAtDesc(userId)) {
			seen.add(HistoryPosting.key(existing.getTitle(), existing.getCompany()));
		}
		List<ApplicationResponse> imported = new ArrayList<>();
		int skipped = 0;
		for (HistoryJobRequest job : jobs) {
			if (HistoryPosting.skip(job)) {
				skipped++;
				continue;
			}
			String title = HistoryPosting.title(job);
			String company = HistoryPosting.company(job);
			if (!seen.add(HistoryPosting.key(title, company))) {
				skipped++;
				continue;
			}
			JobPosting posting = postings
				.save(JobPosting.fromHistory(userId, title, company, HistoryPosting.body(job)));
			Application application = applications.save(Application.create(userId,
					new ApplicationRequest(title, company, HistoryPosting.location(job), HistoryPosting.pay(job),
							"draft", posting.getId(), null, HistoryPosting.notes(job))));
			events.save(ApplicationStatusEvent.record(userId, application.getId(), application.getStatus(), ""));
			imported.add(response(application));
		}
		return new HistoryImportResponse(imported.size(), skipped, imported);
	}

	@Transactional
	public ApplicationResponse create(UUID userId, ApplicationRequest request) {
		String status = Application.statusOf(request.status(), "draft");
		if (status == null) {
			throw new InvalidFieldsException("status", "Use one of the saved application statuses.");
		}
		requirePosting(userId, request.postingId());
		Application application = applications.save(Application.create(userId, request));
		events.save(ApplicationStatusEvent.record(userId, application.getId(), application.getStatus(), ""));
		return response(application);
	}

	@Transactional
	public ApplicationResponse update(UUID userId, UUID id, ApplicationRequest request) {
		Application application = owned(userId, id);
		String status = Application.statusOf(request.status(), application.getStatus());
		if (status == null) {
			throw new InvalidFieldsException("status", "Use one of the saved application statuses.");
		}
		requirePosting(userId, request.postingId());
		String previous = application.getStatus();
		application.apply(request, false);
		applications.save(application);
		if (!previous.equals(application.getStatus())) {
			events.save(ApplicationStatusEvent.record(userId, application.getId(), application.getStatus(),
					application.getNotes()));
		}
		return response(application);
	}

	@Transactional
	public void delete(UUID userId, UUID id) {
		applications.delete(owned(userId, id));
	}

	@Transactional(readOnly = true)
	public List<ApplicationHint> hints(UUID userId) {
		return applications.findByUserIdOrderByCreatedAtDesc(userId)
			.stream()
			.map((application) -> new ApplicationHint(application.getId(), application.getTitle(), application.getCompany()))
			.toList();
	}

	@Transactional
	public ApplicationResponse createFromMail(UUID userId, String title, String company, String status, Instant mailAt,
			String note) {
		String next = Application.statusOf(status, "applied");
		if (next == null) {
			throw new InvalidFieldsException("status", "Use one of the saved application statuses.");
		}
		String heading = title == null || title.isBlank() ? "Email from " + (company == null ? "mailbox" : company.strip())
				: title.strip();
		Application application = applications
			.save(Application.fromMail(userId, heading, company, next, mailAt, note));
		events.save(ApplicationStatusEvent.record(userId, application.getId(), next, note));
		return response(application);
	}

	@Transactional
	public ApplicationResponse markFromMail(UUID userId, UUID id, String status, Instant mailAt, String note) {
		Application application = owned(userId, id);
		String next = Application.statusOf(status, application.getStatus());
		if (next == null) {
			throw new InvalidFieldsException("status", "Use one of the saved application statuses.");
		}
		String previous = application.getStatus();
		application.markFromMail(next, mailAt);
		applications.save(application);
		if (!previous.equals(next)) {
			events.save(ApplicationStatusEvent.record(userId, id, next, note));
		}
		return response(application);
	}

	private Application owned(UUID userId, UUID id) {
		return applications.findByIdAndUserId(id, userId).orElseThrow(NotFoundException::new);
	}

	private void requirePosting(UUID userId, UUID postingId) {
		if (postingId == null) {
			return;
		}
		JobPosting posting = postings.findByIdAndUserId(postingId, userId).orElseThrow(NotFoundException::new);
		if (!posting.getUserId().equals(userId)) {
			throw new NotFoundException();
		}
	}

	private ApplicationResponse response(Application application) {
		return ApplicationResponse.from(application, estimateOf(application),
				events.findByApplicationIdAndUserIdOrderByCreatedAtAsc(application.getId(), application.getUserId()));
	}

	private Integer estimateOf(Application application) {
		if (application.getPostingId() == null) {
			return null;
		}
		List<TailoredResumeSummary> versions = resumes.summaries(application.getPostingId(), application.getUserId());
		return versions.isEmpty() ? null : versions.getFirst().total();
	}

}
