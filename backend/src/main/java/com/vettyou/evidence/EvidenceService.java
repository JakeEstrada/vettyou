package com.vettyou.evidence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vettyou.common.NotFoundException;

@Service
public class EvidenceService {

	private final StackDetector detector;

	private final ReadmeParser parser;

	private final EvidenceClaimRepository claims;

	private final ClaimDecisionRepository decisions;

	public EvidenceService(StackDetector detector, ReadmeParser parser, EvidenceClaimRepository claims,
			ClaimDecisionRepository decisions) {
		this.detector = detector;
		this.parser = parser;
		this.claims = claims;
		this.decisions = decisions;
	}

	/**
	 * Adds a candidate for each technology and each built feature the README lists that this project has not seen
	 * before. Items the user already approved or rejected keep that decision.
	 */
	@Transactional
	public void extractFromReadme(UUID userId, UUID projectId, UUID sourceVersionId, String body) {
		for (StackDetector.Detection detection : detector.detect(body)) {
			if (!claims.existsByProjectIdAndKindAndName(projectId, "skill", detection.name())) {
				claims.save(EvidenceClaim.skill(userId, projectId, sourceVersionId, detection));
			}
		}
		for (ReadmeParser.Feature feature : parser.features(body)) {
			if (!claims.existsByProjectIdAndKindAndName(projectId, "feature", feature.name())) {
				claims.save(EvidenceClaim.feature(userId, projectId, sourceVersionId, feature));
			}
		}
	}

	@Transactional(readOnly = true)
	public List<ClaimResponse> forProject(UUID projectId) {
		return claims.findByProjectIdOrderByCreatedAtAscNameAsc(projectId).stream().map(ClaimResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public List<ClaimView> claimsFor(UUID userId) {
		return claims.findByUserIdOrderByCreatedAtAscLineNumberAsc(userId).stream().map(ClaimView::from).toList();
	}

	/**
	 * Applies one decision to a skill on every project. Keeping only touches items not yet checked, so "Not mine"
	 * stays put. "Not mine" covers kept items too.
	 */
	@Transactional
	public void decideSkill(UUID userId, String name, String decision) {
		List<EvidenceClaim> matching = claims.findByUserIdAndKindAndNameIgnoreCase(userId, "skill", name.strip());
		if (matching.isEmpty()) {
			throw new NotFoundException();
		}
		for (EvidenceClaim claim : matching) {
			boolean change = EvidenceClaim.APPROVED.equals(decision) ? EvidenceClaim.CANDIDATE.equals(claim.getStatus())
					: !EvidenceClaim.REJECTED.equals(claim.getStatus());
			if (change) {
				claim.decide(decision);
				decisions.save(ClaimDecision.of(userId, claim.getId(), decision));
				claims.save(claim);
			}
		}
	}

	@Transactional
	public ClaimResponse decide(UUID userId, UUID claimId, String decision) {
		EvidenceClaim claim = claims.findByIdAndUserId(claimId, userId).orElseThrow(NotFoundException::new);
		claim.decide(decision);
		decisions.save(ClaimDecision.of(userId, claimId, decision));
		return ClaimResponse.from(claims.save(claim));
	}

}
