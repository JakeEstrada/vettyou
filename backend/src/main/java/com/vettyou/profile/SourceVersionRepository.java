package com.vettyou.profile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceVersionRepository extends JpaRepository<SourceVersion, UUID> {

	List<SourceVersion> findByExperienceIdOrderByVersionNumberAsc(UUID experienceId);

	Optional<SourceVersion> findFirstByExperienceIdOrderByVersionNumberDesc(UUID experienceId);

}
