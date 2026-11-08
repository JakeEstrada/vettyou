package com.vettyou.evidence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceClaimRepository extends JpaRepository<EvidenceClaim, UUID> {

	List<EvidenceClaim> findByProjectIdOrderByCreatedAtAscNameAsc(UUID projectId);

	boolean existsByProjectIdAndKindAndName(UUID projectId, String kind, String name);

	Optional<EvidenceClaim> findByIdAndUserId(UUID id, UUID userId);

	List<EvidenceClaim> findByUserIdOrderByCreatedAtAscLineNumberAsc(UUID userId);

	List<EvidenceClaim> findByUserIdAndKindAndNameIgnoreCase(UUID userId, String kind, String name);

}
