package com.vettyou.profile;

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
@RequestMapping("/api/v1/educations")
public class EducationController {

	private final CurrentUser currentUser;

	private final EducationService educations;

	public EducationController(CurrentUser currentUser, EducationService educations) {
		this.currentUser = currentUser;
		this.educations = educations;
	}

	@GetMapping
	public List<EducationResponse> list(@AuthenticationPrincipal UserDetails principal) {
		return educations.list(currentUser.id(principal));
	}

	@PostMapping
	public ResponseEntity<EducationResponse> create(@AuthenticationPrincipal UserDetails principal,
			@Valid @RequestBody EducationRequest request) {
		return ResponseEntity.status(201).body(educations.create(currentUser.id(principal), request));
	}

	@PutMapping("/{id}")
	public EducationResponse update(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id,
			@Valid @RequestBody EducationRequest request) {
		return educations.update(currentUser.id(principal), id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id) {
		educations.delete(currentUser.id(principal), id);
		return ResponseEntity.noContent().build();
	}

}
