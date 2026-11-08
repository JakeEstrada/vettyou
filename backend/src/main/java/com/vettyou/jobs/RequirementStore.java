package com.vettyou.jobs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.vettyou.matching.EvidenceRef;
import com.vettyou.matching.PartInput;
import com.vettyou.matching.PartJudgment;
import com.vettyou.matching.RequirementInput;

/** Saves and loads a posting's requirements with their parts and citations. Callers own the transaction. */
@Component
class RequirementStore {

	private final JobRequirementRepository requirements;

	private final JobRequirementPartRepository parts;

	private final JobPartEvidenceRepository evidence;

	RequirementStore(JobRequirementRepository requirements, JobRequirementPartRepository parts,
			JobPartEvidenceRepository evidence) {
		this.requirements = requirements;
		this.parts = parts;
		this.evidence = evidence;
	}

	List<RequirementInput> inputs(UUID postingId) {
		List<JobRequirement> rows = requirements.findByPostingIdOrderByPositionAsc(postingId);
		Map<UUID, List<JobRequirementPart>> partsByRequirement = parts
			.findByRequirementIdInOrderByPositionAsc(rows.stream().map(JobRequirement::getId).toList())
			.stream()
			.collect(Collectors.groupingBy(JobRequirementPart::getRequirementId, LinkedHashMap::new,
					Collectors.toList()));
		List<UUID> partIds = partsByRequirement.values()
			.stream()
			.flatMap(List::stream)
			.map(JobRequirementPart::getId)
			.toList();
		Map<UUID, List<EvidenceRef>> evidenceByPart = evidence.findByPartIdInOrderByPositionAsc(partIds)
			.stream()
			.collect(Collectors.groupingBy(JobPartEvidence::getPartId, LinkedHashMap::new,
					Collectors.mapping(JobPartEvidence::ref, Collectors.toList())));
		return rows.stream().map(row -> {
			List<PartInput> rowParts = partsByRequirement.getOrDefault(row.getId(), List.of())
				.stream()
				.map(part -> new PartInput(part.getId(), part.getText(), part.getConcept(), part.getSupport(),
						part.getReason(), evidenceByPart.getOrDefault(part.getId(), List.of())))
				.toList();
			if (rowParts.isEmpty()) {
				rowParts = List.of(new PartInput(null, row.getSkill(), concept(row.getSkill()), null, "", List.of()));
			}
			return new RequirementInput(row.getId(), row.getSkill(), row.getKind(), row.getImportance(),
					row.getCategory(), row.getQuote(), row.getLogic(), row.getAnswer(), row.getAnsweredAt(), rowParts);
		}).toList();
	}

	void replace(UUID userId, UUID postingId, List<JobReader.Requirement> read) {
		requirements.deleteByPostingId(postingId);
		int position = 0;
		for (JobReader.Requirement requirement : read) {
			JobRequirement row = requirements.save(JobRequirement.of(userId, postingId, position++, requirement));
			int partPosition = 0;
			for (JobReader.Part part : requirement.parts()) {
				parts.save(JobRequirementPart.of(userId, row.getId(), partPosition++, part.text(), part.concept()));
			}
		}
	}

	/** Postings read before parts existed get one part per requirement, so a re-check has something to judge. */
	void ensureParts(UUID postingId) {
		List<JobRequirement> rows = requirements.findByPostingIdOrderByPositionAsc(postingId);
		List<UUID> withParts = parts.findByRequirementIdInOrderByPositionAsc(rows.stream().map(JobRequirement::getId).toList())
			.stream()
			.map(JobRequirementPart::getRequirementId)
			.distinct()
			.toList();
		List<JobRequirementPart> added = new ArrayList<>();
		for (JobRequirement row : rows) {
			if (!withParts.contains(row.getId())) {
				added.add(JobRequirementPart.of(row.getUserId(), row.getId(), 0, row.getSkill(), concept(row.getSkill())));
			}
		}
		parts.saveAll(added);
	}

	void judge(UUID userId, Map<UUID, PartJudgment> judged) {
		if (judged.isEmpty()) {
			return;
		}
		evidence.deleteByPartIdIn(judged.keySet());
		for (JobRequirementPart part : parts.findAllById(judged.keySet())) {
			PartJudgment judgment = judged.get(part.getId());
			part.judge(judgment.support(), judgment.reason());
			parts.save(part);
			int position = 0;
			for (EvidenceRef ref : judgment.evidence()) {
				evidence.save(JobPartEvidence.of(userId, part.getId(), position++, ref));
			}
		}
	}

	private static String concept(String skill) {
		return skill.strip().toLowerCase(Locale.ROOT);
	}

}
