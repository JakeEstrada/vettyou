package com.vettyou.projects;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectSourceVersionRepository extends JpaRepository<ProjectSourceVersion, UUID> {

	List<ProjectSourceVersion> findByProjectIdOrderByVersionNumberAsc(UUID projectId);

	Optional<ProjectSourceVersion> findFirstByProjectIdOrderByVersionNumberDesc(UUID projectId);
}
