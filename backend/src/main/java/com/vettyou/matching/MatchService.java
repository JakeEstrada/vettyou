package com.vettyou.matching;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.vettyou.ai.OpenAiClient;
import com.vettyou.evidence.StackDetector;

@Service
public class MatchService {

	private static final Logger log = LoggerFactory.getLogger(MatchService.class);

	private static final int CITES = 4;

	private final LibraryLoader loader;

	private final EvidenceMatcher ai;

	private final RulesJudge rules;

	private final MatchScorer scorer;

	private final StackDetector detector;

	public MatchService(LibraryLoader loader, StackDetector detector, EvidenceMatcher ai) {
		this.loader = loader;
		this.ai = ai;
		this.detector = detector;
		this.rules = new RulesJudge(detector);
		this.scorer = new MatchScorer(detector);
	}

	/**
	 * Judges every part against the person's lines with OpenAI, then adds lines that name a listed technology. With no
	 * key, or when the model cannot be reached, it returns no judgments; parts are then judged by the rules so a
	 * resume can still be built. Call outside a transaction. {@code role} is the job title and employer as the posting
	 * states them, or empty.
	 */
	public Judgments judge(UUID userId, String role, List<RequirementInput> requirements) {
		EvidenceIndex index = EvidenceIndex.of(loader.load(userId));
		if (!ai.enabled()) {
			return new Judgments("rules", index.fingerprint(), Map.of());
		}
		Map<UUID, PartJudgment> judged;
		try {
			judged = new LinkedHashMap<>(ai.judge(role, requirements, index));
		}
		catch (OpenAiClient.AiException ex) {
			log.warn("Evidence match fell back to the tech list: {}", ex.getMessage());
			return new Judgments("rules", index.fingerprint(), Map.of());
		}
		for (RequirementInput requirement : requirements) {
			for (PartInput part : requirement.parts()) {
				PartJudgment judgment = judged.get(part.id());
				if (judgment != null) {
					judged.put(part.id(), MetricsRule.check(part, crossCheck(part, judgment, index)));
				}
			}
		}
		return new Judgments(ai.name(), index.fingerprint(), judged);
	}

	/**
	 * Scores requirements against the person's current data. Stored citations are checked again here; parts never
	 * judged get the rules. {@code fingerprint} is what the stored judgments saw, or empty when nothing is stored.
	 */
	public MatchResult match(UUID userId, List<RequirementInput> requirements, String fingerprint) {
		Library library = loader.load(userId);
		EvidenceIndex index = EvidenceIndex.of(library);
		List<RequirementInput> judged = requirements.stream()
			.map(requirement -> withRules(requirement, index, library))
			.toList();
		boolean changed = !fingerprint.isEmpty() && !fingerprint.equals(index.fingerprint());
		return scorer.score(judged, index, library, changed);
	}

	/**
	 * Ranks the person's confirmed lines for a posting and lists their kept skills. {@code posting} is the posting's
	 * text; it only orders the skills and picks their spelling, and none of it is ever put on a resume.
	 */
	public ResumePlan plan(UUID userId, List<RequirementInput> requirements, String fingerprint, String posting) {
		Library library = loader.load(userId);
		EvidenceIndex index = EvidenceIndex.of(library);
		List<RequirementInput> judged = requirements.stream()
			.map(requirement -> withRules(requirement, index, library))
			.toList();
		boolean changed = !fingerprint.isEmpty() && !fingerprint.equals(index.fingerprint());
		MatchResult match = scorer.score(judged, index, library, changed);
		return ResumePlanner.plan(match, index, library, detector, posting);
	}

	/**
	 * Scores what a resume shows: the lines whose keys are given, every job title and school, and the rows of its
	 * skills section. {@code readability} is the share of read-back checks its PDF passed.
	 */
	public MatchResult scoreResume(UUID userId, List<RequirementInput> requirements, Set<String> lineKeys,
			List<String> skillRows, double readability) {
		Library library = loader.load(userId);
		EvidenceIndex all = EvidenceIndex.of(library);
		EvidenceIndex shown = all.onResume(lineKeys, skillRows);
		List<RequirementInput> judged = requirements.stream()
			.map(requirement -> onResume(requirement, shown, all, library))
			.toList();
		return scorer.scoreResume(judged, shown, all, library, readability);
	}

	/**
	 * Stored judgments keep their citations, and a listed technology also counts where the resume names it, such as
	 * its skills section. Parts judged by the rules are judged on the resume; when it shows nothing, they are judged on
	 * all saved lines too, so evidence the resume left off reads as left off rather than missing.
	 */
	private RequirementInput onResume(RequirementInput requirement, EvidenceIndex shown, EvidenceIndex all,
			Library library) {
		List<PartInput> parts = requirement.parts().stream().map(part -> {
			PartJudgment judgment;
			if (part.support() != null) {
				judgment = crossCheck(part, new PartJudgment(part.support(), part.reason(), part.evidence()), shown,
						Integer.MAX_VALUE);
			}
			else {
				judgment = rules.judge(requirement, part, shown, library);
				if (!supported(judgment)) {
					PartJudgment saved = rules.judge(requirement, part, all, library);
					if (supported(saved)) {
						judgment = saved;
					}
				}
			}
			judgment = MetricsRule.check(part, judgment);
			return new PartInput(part.id(), part.text(), part.concept(), judgment.support(), judgment.reason(),
					judgment.evidence());
		}).toList();
		return new RequirementInput(requirement.id(), requirement.skill(), requirement.kind(), requirement.importance(),
				requirement.category(), requirement.quote(), requirement.logic(), requirement.answer(),
				requirement.answeredAt(), parts);
	}

	private static boolean supported(PartJudgment judgment) {
		return ("direct".equals(judgment.support()) || "transferable".equals(judgment.support()))
				&& !judgment.evidence().isEmpty();
	}

	private PartJudgment crossCheck(PartInput part, PartJudgment judgment, EvidenceIndex index) {
		return crossCheck(part, judgment, index, CITES);
	}

	/** A part that is just a listed technology counts wherever the person's own lines name it. */
	private PartJudgment crossCheck(PartInput part, PartJudgment judgment, EvidenceIndex index, int cites) {
		Optional<Pattern> pattern = rules.techPattern(part.text());
		if (pattern.isEmpty()) {
			return judgment;
		}
		List<EvidenceRef> found = RulesJudge.find(index, pattern.get());
		if (found.isEmpty()) {
			return judgment;
		}
		List<EvidenceRef> evidence = new ArrayList<>(judgment.evidence());
		for (EvidenceRef ref : found) {
			boolean known = evidence.stream()
				.anyMatch(existing -> existing.sourceId().equals(ref.sourceId()) && existing.line().equals(ref.line()));
			if (!known && evidence.size() < cites) {
				evidence.add(ref);
			}
		}
		String reason = "direct".equals(judgment.support()) ? judgment.reason() : "Named in your own lines.";
		return new PartJudgment("direct", reason, List.copyOf(evidence));
	}

	/** Parts never judged get the rules. Every judgment, stored or new, must pass the metrics rule. */
	private RequirementInput withRules(RequirementInput requirement, EvidenceIndex index, Library library) {
		List<PartInput> parts = requirement.parts()
			.stream()
			.map(part -> {
				PartJudgment judgment = MetricsRule.check(part, part.support() != null
						? new PartJudgment(part.support(), part.reason(), part.evidence())
						: rules.judge(requirement, part, index, library));
				return new PartInput(part.id(), part.text(), part.concept(), judgment.support(), judgment.reason(),
						judgment.evidence());
			})
			.toList();
		return new RequirementInput(requirement.id(), requirement.skill(), requirement.kind(), requirement.importance(),
				requirement.category(), requirement.quote(), requirement.logic(), requirement.answer(),
				requirement.answeredAt(), parts);
	}

}
