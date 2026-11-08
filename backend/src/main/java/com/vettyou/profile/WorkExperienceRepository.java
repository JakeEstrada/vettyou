package com.vettyou.profile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkExperienceRepository extends JpaRepository<WorkExperience, UUID> {

	List<WorkExperience> findByUserIdOrderByCreatedAtDesc(UUID userId);

	Optional<WorkExperience> findByIdAndUserId(UUID id, UUID userId);

}
