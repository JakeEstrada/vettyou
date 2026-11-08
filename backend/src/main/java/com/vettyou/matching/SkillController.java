package com.vettyou.matching;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vettyou.auth.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {

	private final CurrentUser currentUser;

	private final SkillService skills;

	public SkillController(CurrentUser currentUser, SkillService skills) {
		this.currentUser = currentUser;
		this.skills = skills;
	}

	@GetMapping
	public List<SkillItem> list(@AuthenticationPrincipal UserDetails principal) {
		return skills.skills(currentUser.id(principal));
	}

	@PostMapping("/decision")
	public List<SkillItem> decide(@AuthenticationPrincipal UserDetails principal,
			@Valid @RequestBody SkillDecisionRequest request) {
		return skills.decide(currentUser.id(principal), request);
	}

}
