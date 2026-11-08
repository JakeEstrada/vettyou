package com.vettyou.jobs;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.vettyou.ai.OpenAiClient;
import com.vettyou.matching.Judgments;
import com.vettyou.matching.MatchService;
import com.vettyou.matching.RequirementInput;

import jakarta.annotation.PreDestroy;

/**
 * Reads postings and matches them to evidence off the request thread. Model calls happen outside any transaction.
 * {@code reading} runs both steps; {@code matching} only re-checks the saved requirements.
 */
@Component
class JobReadWorker {

	private static final Logger log = LoggerFactory.getLogger(JobReadWorker.class);

	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	private final JobPostingRepository postings;

	private final RequirementStore store;

	private final JobReader reader;

	private final MatchService matching;

	private final TransactionTemplate transactions;

	JobReadWorker(JobPostingRepository postings, RequirementStore store, JobReader reader, MatchService matching,
			TransactionTemplate transactions) {
		this.postings = postings;
		this.store = store;
		this.reader = reader;
		this.matching = matching;
		this.transactions = transactions;
	}

	void submit(UUID postingId) {
		executor.execute(() -> run(postingId));
	}

	@EventListener(ApplicationReadyEvent.class)
	void resumeUnfinished() {
		List<JobPosting> waiting = postings.findByStatusIn(List.of(JobPosting.READING, JobPosting.MATCHING));
		waiting.forEach(posting -> submit(posting.getId()));
		if (!waiting.isEmpty()) {
			log.info("Re-queued {} postings left reading or matching", waiting.size());
		}
	}

	private void run(UUID postingId) {
		JobPosting posting = postings.findById(postingId).orElse(null);
		if (posting == null || !posting.busy()) {
			return;
		}
		try {
			if (JobPosting.READING.equals(posting.getStatus())) {
				JobReader.Result result = reader.read(posting.getBody());
				transactions.executeWithoutResult(status -> saveRead(postingId, result));
			}
			Matching work = transactions.execute(status -> {
				store.ensureParts(postingId);
				return new Matching(role(postings.findById(postingId).orElse(posting)), store.inputs(postingId));
			});
			Judgments judgments = matching.judge(posting.getUserId(), work.role(), work.inputs());
			transactions.executeWithoutResult(status -> saveMatch(postingId, judgments));
		}
		catch (OpenAiClient.AiException ex) {
			transactions.executeWithoutResult(status -> fail(postingId, ex.getMessage()));
		}
		catch (RuntimeException ex) {
			log.error("Reading posting {} failed", postingId, ex);
			transactions.executeWithoutResult(status -> fail(postingId, "Something went wrong reading this posting."));
		}
	}

	private record Matching(String role, List<RequirementInput> inputs) {
	}

	/** "Strategic Project Lead, Coding/SWE at Crossing Hurdles", or whichever half the posting states. */
	private static String role(JobPosting posting) {
		String title = PostingCopy.title(posting.getTitle(), posting.getCompany(), posting.getBody());
		String company = posting.getCompany().strip();
		return title.isEmpty() || company.isEmpty() ? title + company : title + " at " + company;
	}

	private void saveRead(UUID postingId, JobReader.Result result) {
		postings.findById(postingId).ifPresent(posting -> {
			store.replace(posting.getUserId(), postingId, result.requirements());
			posting.read(result.title(), result.company(), result.reader());
			postings.save(posting);
		});
	}

	private void saveMatch(UUID postingId, Judgments judgments) {
		postings.findById(postingId).filter(JobPosting::busy).ifPresent(posting -> {
			store.judge(posting.getUserId(), judgments.parts());
			posting.matched(judgments.matcher(), judgments.fingerprint());
			postings.save(posting);
		});
	}

	private void fail(UUID postingId, String message) {
		postings.findById(postingId).ifPresent(posting -> {
			posting.fail(message);
			postings.save(posting);
		});
	}

	@PreDestroy
	void stop() {
		executor.shutdownNow();
	}

}
