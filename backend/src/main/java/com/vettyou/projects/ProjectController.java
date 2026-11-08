package com.vettyou.projects;

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
@RequestMapping("/api/v1/projects")
public class ProjectController {

	private final CurrentUser currentUser;

	private final ProjectService projects;

	public ProjectController(CurrentUser currentUser, ProjectService projects) {
		this.currentUser = currentUser;
		this.projects = projects;
	}

	@GetMapping
	public List<ProjectSummary> list(@AuthenticationPrincipal UserDetails principal) {
		return projects.list(currentUser.id(principal));
	}

	@PostMapping("/import")
	public ResponseEntity<ProjectResponse> importReadme(@AuthenticationPrincipal UserDetails principal,
			@Valid @RequestBody ReadmeUploadRequest request) {
		return ResponseEntity.status(201).body(projects.importReadme(currentUser.id(principal), request));
	}

	@GetMapping("/{id}")
	public ProjectResponse get(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id) {
		return projects.get(currentUser.id(principal), id);
	}

	@PutMapping("/{id}")
	public ProjectResponse update(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id,
			@Valid @RequestBody ProjectDetailsRequest request) {
		return projects.update(currentUser.id(principal), id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id) {
		projects.delete(currentUser.id(principal), id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/reread")
	public ProjectResponse reread(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id) {
		return projects.reread(currentUser.id(principal), id);
	}

	@PostMapping("/{id}/sources")
	public ProjectResponse addReadme(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id,
			@Valid @RequestBody ReadmeUploadRequest request) {
		return projects.addReadme(currentUser.id(principal), id, request);
	}

}
