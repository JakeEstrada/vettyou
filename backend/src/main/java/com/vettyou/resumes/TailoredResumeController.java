package com.vettyou.resumes;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vettyou.auth.CurrentUser;

@RestController
@RequestMapping("/api/v1/jobs/{postingId}/resumes")
public class TailoredResumeController {

	/** A saved version never changes, so its files can be cached by the person's own browser. */
	private static final CacheControl IMMUTABLE = CacheControl.maxAge(Duration.ofHours(1)).cachePrivate();

	private final CurrentUser currentUser;

	private final TailoredResumeService resumes;

	public TailoredResumeController(CurrentUser currentUser, TailoredResumeService resumes) {
		this.currentUser = currentUser;
		this.resumes = resumes;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TailoredResumeResponse build(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID postingId) {
		return resumes.build(currentUser.id(principal), postingId);
	}

	@GetMapping
	public List<TailoredResumeSummary> list(@AuthenticationPrincipal UserDetails principal,
			@PathVariable UUID postingId) {
		return resumes.list(currentUser.id(principal), postingId);
	}

	@GetMapping("/{id}")
	public TailoredResumeResponse get(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID postingId,
			@PathVariable UUID id) {
		return resumes.get(currentUser.id(principal), postingId, id);
	}

	@GetMapping("/{id}/resume.pdf")
	public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID postingId,
			@PathVariable UUID id) {
		TailoredResumeService.ResumeFile file = resumes.pdf(currentUser.id(principal), postingId, id);
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_PDF)
			.cacheControl(IMMUTABLE)
			.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(file.name()).build().toString())
			.body(file.content());
	}

	@GetMapping("/{id}/preview.png")
	public ResponseEntity<byte[]> preview(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID postingId,
			@PathVariable UUID id) {
		return ResponseEntity.ok()
			.contentType(MediaType.IMAGE_PNG)
			.cacheControl(IMMUTABLE)
			.body(resumes.preview(currentUser.id(principal), postingId, id));
	}

}
