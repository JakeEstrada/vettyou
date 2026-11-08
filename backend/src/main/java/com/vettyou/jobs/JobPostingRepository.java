package com.vettyou.jobs;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {

	List<JobPosting> findByUserIdOrderByCreatedAtDesc(UUID userId);

	Optional<JobPosting> findByIdAndUserId(UUID id, UUID userId);

	List<JobPosting> findByStatusIn(Collection<String> statuses);

	long countByUserIdAndStatusIn(UUID userId, Collection<String> statuses);

}
