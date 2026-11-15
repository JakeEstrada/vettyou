package com.vettyou.jobs;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vettyou.auth.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

	private final CurrentUser currentUser;

	private final JobService jobs;

	public JobController(CurrentUser currentUser, JobService jobs) {
		this.currentUser = currentUser;
		this.jobs = jobs;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public JobPostingSummary create(@AuthenticationPrincipal UserDetails principal,
			@Valid @RequestBody JobPostingRequest request) {
		return jobs.create(currentUser.id(principal), request);
	}

	@GetMapping
	public List<JobPostingSummary> list(@AuthenticationPrincipal UserDetails principal) {
		return jobs.list(currentUser.id(principal));
	}

	@GetMapping("/{id}")
	public JobPostingResponse get(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id) {
		return jobs.get(currentUser.id(principal), id);
	}

	@GetMapping(value = "/{id}/report.txt", produces = "text/plain;charset=UTF-8")
	public String reportText(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id) {
		return jobs.reportText(currentUser.id(principal), id);
	}

	@GetMapping("/{id}/report.pdf")
	public ResponseEntity<byte[]> reportPdf(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id) {
		byte[] pdf = jobs.reportPdf(currentUser.id(principal), id);
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_PDF)
			.header(HttpHeaders.CONTENT_DISPOSITION,
					ContentDisposition.inline().filename("vettyou-job-match-report.pdf").build().toString())
			.body(pdf);
	}

	@GetMapping(value = "/{id}/posting.md", produces = "text/markdown;charset=UTF-8")
	public ResponseEntity<String> postingMarkdown(@AuthenticationPrincipal UserDetails principal,
			@PathVariable UUID id) {
		String body = jobs.postingMarkdown(currentUser.id(principal), id);
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION,
					ContentDisposition.attachment().filename("job.md").build().toString())
			.body(body);
	}

	@PostMapping("/{id}/read")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public JobPostingSummary reread(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id) {
		return jobs.reread(currentUser.id(principal), id);
	}

	@PostMapping("/{id}/match")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public JobPostingSummary rematch(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id) {
		return jobs.rematch(currentUser.id(principal), id);
	}

	@PutMapping("/{id}/requirements/{requirementId}/answer")
	public JobPostingResponse answer(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id,
			@PathVariable UUID requirementId, @Valid @RequestBody AnswerRequest request) {
		return jobs.answer(currentUser.id(principal), id, requirementId, request);
	}

	@DeleteMapping("/{id}/requirements/{requirementId}")
	public JobPostingResponse removeRequirement(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id,
			@PathVariable UUID requirementId) {
		return jobs.removeRequirement(currentUser.id(principal), id, requirementId);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id) {
		jobs.delete(currentUser.id(principal), id);
	}

}
