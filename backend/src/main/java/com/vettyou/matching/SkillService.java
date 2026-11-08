package com.vettyou.matching;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.vettyou.evidence.ClaimView;
import com.vettyou.evidence.EvidenceService;
import com.vettyou.evidence.StackDetector;
import com.vettyou.profile.ExperienceText;

@Service
public class SkillService {

	private static final List<String> STATUS_ORDER = List.of("kept", "review", "planned", "rejected");

	private final LibraryLoader loader;

	private final StackDetector detector;

	private final EvidenceService evidence;

	public SkillService(LibraryLoader loader, StackDetector detector, EvidenceService evidence) {
		this.loader = loader;
		this.detector = detector;
		this.evidence = evidence;
	}

	public List<SkillItem> skills(UUID userId) {
		return build(loader.load(userId), detector);
	}

	public List<SkillItem> decide(UUID userId, SkillDecisionRequest request) {
		evidence.decideSkill(userId, request.name(), request.decision());
		return skills(userId);
	}

	static List<SkillItem> build(Library library, StackDetector detector) {
		Map<String, Tally> byName = new LinkedHashMap<>();
		for (ClaimView claim : library.claims()) {
			if (!"skill".equals(claim.kind())) {
				continue;
			}
			Tally tally = byName.computeIfAbsent(key(claim.name()), key -> new Tally(claim.name(), claim.category()));
			String label = library.projectName(claim.projectId()) + (claim.planned() ? " (planned)" : "");
			tally.uses.add(new SkillUse("project", claim.projectId(), label, claim.quote(), claim.status()));
			if (claim.approved() && !claim.planned()) {
				tally.backing.add(claim.projectId());
			}
			tally.pending |= claim.candidate();
			tally.planned |= claim.approved() && claim.planned();
		}
		for (ExperienceText job : library.jobs()) {
			for (StackDetector.Detection detection : detector.detect(job.title() + "\n" + job.body())) {
				Tally tally = byName.computeIfAbsent(key(detection.name()),
						key -> new Tally(detection.name(), detection.category()));
				tally.uses.add(new SkillUse("job", job.id(), job.label(), detection.quote(), "written"));
				tally.backing.add(job.id());
			}
		}
		return byName.values().stream()
			.map(Tally::item)
			.sorted(Comparator.<SkillItem>comparingInt(item -> STATUS_ORDER.indexOf(item.status()))
				.thenComparing(Comparator.comparingInt(SkillItem::places).reversed())
				.thenComparing(item -> item.name().toLowerCase(Locale.ROOT)))
			.toList();
	}

	private static String key(String name) {
		return name.strip().toLowerCase(Locale.ROOT);
	}

	private static final class Tally {

		private final String name;

		private final String category;

		private final List<SkillUse> uses = new ArrayList<>();

		private final Set<UUID> backing = new LinkedHashSet<>();

		private boolean pending;

		private boolean planned;

		private Tally(String name, String category) {
			this.name = name;
			this.category = category;
		}

		private SkillItem item() {
			String status = !backing.isEmpty() ? "kept" : pending ? "review" : planned ? "planned" : "rejected";
			return new SkillItem(name, category, status, backing.size(), List.copyOf(uses));
		}

	}

}
