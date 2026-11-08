package com.vettyou.applications;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

	List<Application> findByUserIdOrderByCreatedAtDesc(UUID userId);

	Optional<Application> findByIdAndUserId(UUID id, UUID userId);

}
