package com.vettyou.jobs;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRequirementPartRepository extends JpaRepository<JobRequirementPart, UUID> {

	List<JobRequirementPart> findByRequirementIdInOrderByPositionAsc(Collection<UUID> requirementIds);

}
