package com.vettyou.jobs;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.vettyou.common.InvalidFieldsException;
import com.vettyou.common.NotFoundException;
import com.vettyou.matching.MatchResult;
import com.vettyou.matching.MatchService;
import com.vettyou.matching.RequirementMatch;

@Service
public class JobService {

	private static final int MAX_BUSY = 3;

	private static final List<String> BUSY = List.of(JobPosting.READING, JobPosting.MATCHING);

	private final JobPostingRepository postings;

	private final JobRequirementRepository requirements;

	private final RequirementStore store;

	private final JobReadWorker worker;

	private final MatchService matching;

	private final JobReport report;

	public JobService(JobPostingRepository postings, JobRequirementRepository requirements, RequirementStore store,
			JobReadWorker worker, MatchService matching, JobReport report) {
		this.postings = postings;
		this.requirements = requirements;
		this.store = store;
		this.worker = worker;
		this.matching = matching;
		this.report = report;
	}

	@Transactional
	public JobPostingSummary create(UUID userId, JobPostingRequest request) {
		limitBusy(userId);
		JobPosting posting = postings.save(JobPosting.create(userId, request.body().strip()));
		runAfterCommit(posting.getId());
		return JobPostingSummary.from(posting);
	}

	@Transactional(readOnly = true)
	public List<JobPostingSummary> list(UUID userId) {
		return postings.findByUserIdOrderByCreatedAtDesc(userId).stream().map(JobPostingSummary::from).toList();
	}

	@Transactional(readOnly = true)
	public JobPostingResponse get(UUID userId, UUID id) {
		return response(owned(userId, id));
	}

	@Transactional(readOnly = true)
	public String reportText(UUID userId, UUID id) {
		return report.text(ready(userId, id));
	}

	@Transactional(readOnly = true)
	public byte[] reportPdf(UUID userId, UUID id) {
		return report.pdf(ready(userId, id));
	}

	/** Reads the posting again from scratch. Requirements are replaced, so answers are cleared. */
	@Transactional
	public JobPostingSummary reread(UUID userId, UUID id) {
		JobPosting posting = notBusy(owned(userId, id));
		limitBusy(userId);
		posting.startReading();
		runAfterCommit(posting.getId());
		return JobPostingSummary.from(postings.save(posting));
	}

	/** Checks the saved requirements against the person's current evidence again. Answers are kept. */
	@Transactional
	public JobPostingSummary rematch(UUID userId, UUID id) {
		JobPosting posting = notBusy(owned(userId, id));
		limitBusy(userId);
		posting.startMatching();
		runAfterCommit(posting.getId());
		return JobPostingSummary.from(postings.save(posting));
	}

	@Transactional
	public JobPostingResponse answer(UUID userId, UUID id, UUID requirementId, AnswerRequest request) {
		JobPosting posting = owned(userId, id);
		JobRequirement requirement = requirements.findByIdAndPostingIdAndUserId(requirementId, id, userId)
			.orElseThrow(NotFoundException::new);
		if ("meets".equals(request.answer()) && !canMarkMet(posting, requirementId)) {
			throw new InvalidFieldsException("answer", "Only items Vettyou can't check from text can be marked as met. "
					+ "Add a job or project line that shows it instead.");
		}
		requirement.answer(request.answer());
		requirements.saveAndFlush(requirement);
		return response(posting);
	}

	@Transactional
	public JobPostingResponse removeRequirement(UUID userId, UUID id, UUID requirementId) {
		JobPosting posting = owned(userId, id);
		requirements.delete(requirements.findByIdAndPostingIdAndUserId(requirementId, id, userId)
			.orElseThrow(NotFoundException::new));
		requirements.flush();
		return response(posting);
	}

	@Transactional
	public void delete(UUID userId, UUID id) {
		postings.delete(owned(userId, id));
	}

	@Transactional(readOnly = true)
	public void requireOwned(UUID userId, UUID id) {
		owned(userId, id);
	}

	@Transactional(readOnly = true)
	public Tailoring tailoring(UUID userId, UUID id) {
		JobPosting posting = owned(userId, id);
		return new Tailoring(posting.getId(), posting.getTitle(), posting.getCompany(), posting.getBody(),
				posting.getStatus(), posting.getMatchedAt(), fingerprint(posting), store.inputs(posting.getId()));
	}

	private boolean canMarkMet(JobPosting posting, UUID requirementId) {
		return response(posting).requirements()
			.stream()
			.filter(match -> match.id().equals(requirementId))
			.anyMatch(RequirementMatch::canMarkMet);
	}

	private JobPostingResponse ready(UUID userId, UUID id) {
		JobPosting posting = owned(userId, id);
		if (!JobPosting.READY.equals(posting.getStatus())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This posting has not been read yet.");
		}
		return response(posting);
	}

	private static String fingerprint(JobPosting posting) {
		return posting.getMatcher().startsWith("openai:") ? posting.getEvidenceHash() : "";
	}

	private JobPostingResponse response(JobPosting posting) {
		MatchResult match = matching.match(posting.getUserId(), store.inputs(posting.getId()), fingerprint(posting));
		String company = PostingCopy.company(posting.getCompany(), posting.getBody());
		String title = PostingCopy.title(posting.getTitle(), company, posting.getBody());
		return new JobPostingResponse(posting.getId(), title, company, PostingCopy.pay(posting.getBody()),
				PostingCopy.preview(posting.getBody(), title, company), posting.getStatus(),
				posting.getReader(), posting.getMatcher(), posting.getMatchedAt(), match.evidenceChanged(),
				posting.getError(), posting.getBody(), posting.getCreatedAt(), match.requirements(), match.score());
	}

	private JobPosting notBusy(JobPosting posting) {
		if (posting.busy()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This posting is still being read.");
		}
		return posting;
	}

	private void limitBusy(UUID userId) {
		if (postings.countByUserIdAndStatusIn(userId, BUSY) >= MAX_BUSY) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
					"A few postings are still being read. Wait for them to finish.");
		}
	}

	private void runAfterCommit(UUID postingId) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				worker.submit(postingId);
			}
		});
	}

	private JobPosting owned(UUID userId, UUID id) {
		return postings.findByIdAndUserId(id, userId).orElseThrow(NotFoundException::new);
	}

}
