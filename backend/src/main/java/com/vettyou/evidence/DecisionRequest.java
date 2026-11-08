package com.vettyou.evidence;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DecisionRequest(
		@NotNull(message = "Choose approved or rejected") @Pattern(regexp = "approved|rejected", message = "Choose approved or rejected") String decision) {
}
