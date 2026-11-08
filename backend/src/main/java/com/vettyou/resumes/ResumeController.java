package com.vettyou.resumes;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vettyou.auth.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

	private final CurrentUser currentUser;

	private final ResumeImportService imports;

	public ResumeController(CurrentUser currentUser, ResumeImportService imports) {
		this.currentUser = currentUser;
		this.imports = imports;
	}

	@PostMapping("/import")
	public ResponseEntity<ResumeImportResponse> importResume(@AuthenticationPrincipal UserDetails principal,
			@Valid @RequestBody ResumeUploadRequest request) {
		return ResponseEntity.status(201)
			.body(imports.importResume(currentUser.id(principal), principal.getUsername(), request));
	}

}
