package com.vettyou.jobs;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface JobPartEvidenceRepository extends JpaRepository<JobPartEvidence, UUID> {

	List<JobPartEvidence> findByPartIdInOrderByPositionAsc(Collection<UUID> partIds);

	@Modifying
	@Query("delete from JobPartEvidence evidence where evidence.partId in :partIds")
	void deleteByPartIdIn(Collection<UUID> partIds);

}
