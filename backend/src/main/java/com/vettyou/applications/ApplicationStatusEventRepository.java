package com.vettyou.applications;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationStatusEventRepository extends JpaRepository<ApplicationStatusEvent, UUID> {

	List<ApplicationStatusEvent> findByApplicationIdAndUserIdOrderByCreatedAtAsc(UUID applicationId, UUID userId);

}
