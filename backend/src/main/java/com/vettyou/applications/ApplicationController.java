package com.vettyou.applications;

import java.util.List;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.RestController;

import com.vettyou.auth.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

	private final CurrentUser currentUser;

	private final ApplicationService applications;

	public ApplicationController(CurrentUser currentUser, ApplicationService applications) {
		this.currentUser = currentUser;
		this.applications = applications;
	}

	@GetMapping
	public List<ApplicationResponse> list(@AuthenticationPrincipal UserDetails principal) {
		return applications.list(currentUser.id(principal));
	}

	@PostMapping("/history")
	public HistoryImportResponse importHistory(@AuthenticationPrincipal UserDetails principal,
			@Valid @RequestBody HistoryImportRequest request) {
		return applications.importHistory(currentUser.id(principal), request);
	}

	@PostMapping
	public ResponseEntity<ApplicationResponse> create(@AuthenticationPrincipal UserDetails principal,
			@Valid @RequestBody ApplicationRequest request) {
		return ResponseEntity.status(201).body(applications.create(currentUser.id(principal), request));
	}

	@PutMapping("/{id}")
	public ApplicationResponse update(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id,
			@Valid @RequestBody ApplicationRequest request) {
		return applications.update(currentUser.id(principal), id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id) {
		applications.delete(currentUser.id(principal), id);
		return ResponseEntity.noContent().build();
	}

}
