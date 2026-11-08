package com.vettyou.evidence;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vettyou.auth.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/evidence")
public class EvidenceController {

	private final CurrentUser currentUser;

	private final EvidenceService evidence;

	public EvidenceController(CurrentUser currentUser, EvidenceService evidence) {
		this.currentUser = currentUser;
		this.evidence = evidence;
	}

	@PostMapping("/{id}/decision")
	public ClaimResponse decide(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id,
			@Valid @RequestBody DecisionRequest request) {
		return evidence.decide(currentUser.id(principal), id, request.decision());
	}

}
