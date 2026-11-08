package com.vettyou.applications;

import java.time.Instant;
import java.util.UUID;

public record ApplicationEventResponse(UUID id, String status, String note, Instant createdAt) {

	static ApplicationEventResponse from(ApplicationStatusEvent event) {
		return new ApplicationEventResponse(event.getId(), event.getStatus(), event.getNote(), event.getCreatedAt());
	}

}
