package com.vettyou.evidence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimDecisionRepository extends JpaRepository<ClaimDecision, UUID> {

	long countByClaimId(UUID claimId);

}
