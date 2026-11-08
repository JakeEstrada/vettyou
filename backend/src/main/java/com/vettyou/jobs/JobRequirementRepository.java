package com.vettyou.jobs;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface JobRequirementRepository extends JpaRepository<JobRequirement, UUID> {

	List<JobRequirement> findByPostingIdOrderByPositionAsc(UUID postingId);

	Optional<JobRequirement> findByIdAndPostingIdAndUserId(UUID id, UUID postingId, UUID userId);

	@Modifying
	@Query("delete from JobRequirement requirement where requirement.postingId = :postingId")
	void deleteByPostingId(UUID postingId);

}
