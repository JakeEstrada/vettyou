package com.vettyou.matching;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SkillDecisionRequest(
		@NotBlank(message = "Name the skill") @Size(max = 200, message = "Skill name is too long") String name,
		@NotNull(message = "Choose approved or rejected") @Pattern(regexp = "approved|rejected", message = "Choose approved or rejected") String decision) {
}
