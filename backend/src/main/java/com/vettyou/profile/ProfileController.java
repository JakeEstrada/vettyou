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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class ProfileController {

	private final ProfileService profiles;

	private final ExperienceService experiences;

	public ProfileController(ProfileService profiles, ExperienceService experiences) {
		this.profiles = profiles;
		this.experiences = experiences;
	}

	@GetMapping("/profile")
	public ProfileResponse profile(@AuthenticationPrincipal UserDetails principal) {
		return profiles.get(principal.getUsername());
	}

	@PutMapping("/profile")
	public ProfileResponse saveProfile(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody ProfileRequest request) {
		return profiles.save(principal.getUsername(), request);
	}

	@GetMapping("/experiences")
	public List<ExperienceSummary> experiences(@AuthenticationPrincipal UserDetails principal) {
		return experiences.list(principal.getUsername());
	}

	@PostMapping("/experiences")
	public ResponseEntity<ExperienceResponse> createExperience(@AuthenticationPrincipal UserDetails principal,
			@Valid @RequestBody ExperienceRequest request) {
		return ResponseEntity.status(201).body(experiences.create(principal.getUsername(), request));
	}

	@GetMapping("/experiences/{id}")
	public ExperienceResponse experience(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id) {
		return experiences.get(principal.getUsername(), id);
	}

	@PutMapping("/experiences/{id}")
	public ExperienceResponse saveExperience(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id,
			@Valid @RequestBody ExperienceRequest request) {
		return experiences.update(principal.getUsername(), id, request);
	}

	@DeleteMapping("/experiences/{id}")
	public ResponseEntity<Void> deleteExperience(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id) {
		experiences.delete(principal.getUsername(), id);
		return ResponseEntity.noContent().build();
	}

}
