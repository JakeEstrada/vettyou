package com.vettyou.matching;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.vettyou.evidence.StackDetector;
import com.vettyou.profile.EducationResponse;
import com.vettyou.profile.ExperienceText;
import com.vettyou.profile.ProfileResponse;

/**
 * Orders the person's lines for one posting. Lines are only ever chosen from what the person saved; the posting decides
 * the order, never the words. A line comes first when it covers the most of the posting that lines before it do not,
 * weighted like the score, so five lines proving one requirement do not crowd out the only line proving another.
 * Projects are also ranked on their own lines alone, so a project that shows what a job already shows can still go on.
 */
final class ResumePlanner {

	private static final double EPSILON = 1e-9;

	private static final Map<String, Integer> GROUP_WEIGHTS = Map.of("required", MatchScorer.REQUIRED_WEIGHT,
			"responsibility", MatchScorer.RESPONSIBILITY_WEIGHT, "preferred", MatchScorer.PREFERRED_WEIGHT);

	private static final int MAX_TO_CONFIRM = 12;

	private ResumePlanner() {
	}

	static ResumePlan plan(MatchResult match, EvidenceIndex index, Library library, StackDetector detector,
			String posting) {
		Coverage coverage = Coverage.of(match);
		double[] covered = new double[coverage.size()];
		Map<String, Double> quality = new HashMap<>();
		for (EvidenceLine line : index.lines()) {
			quality.put(line.key(), EvidenceQuality.line(line.text(), detector));
			if (line.heading()) {
				coverage.apply(line.key(), covered);
			}
		}

		List<EvidenceLine> candidates = index.lines()
			.stream()
			.filter(line -> !line.heading() && line.confirmed())
			.toList();
		List<RankedLine> ranked = new ArrayList<>();
		Set<String> taken = new HashSet<>();
		for (Gain gain : greedy(candidates.stream().filter(ResumePlanner::printable).toList(), coverage, covered,
				quality)) {
			taken.add(gain.line().key());
			ranked.add(ranked(gain.line(), gain.value(), quality, coverage));
		}
		candidates.stream()
			.filter(line -> !taken.contains(line.key()))
			.sorted(Comparator.comparing((EvidenceLine line) -> !line.whole())
				.thenComparing(line -> -quality.get(line.key())))
			.forEach(line -> ranked.add(ranked(line, 0, quality, coverage)));

		List<ResumeSkill> skills = skills(library, detector, posting);
		return new ResumePlan(match, List.copyOf(ranked), projects(candidates, coverage, quality, library),
				toConfirm(index, coverage, covered, quality, library), skills, sources(library, index, skills, match),
				library.profile(), List.copyOf(library.jobs()), List.copyOf(library.schools()),
				Map.copyOf(library.projectNames()));
	}

	private record Gain(EvidenceLine line, double value) {
	}

	/** Short enough to print, and not a note. Other lines can still back the score; a resume just never shows them. */
	private static boolean printable(EvidenceLine line) {
		return line.whole() && !RankedLine.noteLike(line.text());
	}

	/** Takes the line that adds the most to {@code covered}, the better-worded on a tie, until none adds anything. */
	private static List<Gain> greedy(List<EvidenceLine> lines, Coverage coverage, double[] covered,
			Map<String, Double> quality) {
		List<Gain> picked = new ArrayList<>();
		Set<String> taken = new HashSet<>();
		while (true) {
			EvidenceLine best = null;
			double bestGain = 0;
			for (EvidenceLine line : lines) {
				if (taken.contains(line.key())) {
					continue;
				}
				double gain = coverage.gain(line.key(), covered);
				if (gain <= EPSILON) {
					continue;
				}
				if (best == null || gain > bestGain + EPSILON || Math.abs(gain - bestGain) <= EPSILON
						&& quality.get(line.key()) > quality.get(best.key()) + EPSILON) {
					best = line;
					bestGain = gain;
				}
			}
			if (best == null) {
				return picked;
			}
			taken.add(best.key());
			coverage.apply(best.key(), covered);
			picked.add(new Gain(best, bestGain));
		}
	}

	/**
	 * Projects whose printable lines show part of the posting, measured on their own so a project counts even where a
	 * job shows the same.
	 */
	private static List<RankedProject> projects(List<EvidenceLine> lines, Coverage coverage,
			Map<String, Double> quality, Library library) {
		Map<UUID, List<EvidenceLine>> byProject = new LinkedHashMap<>();
		for (EvidenceLine line : lines) {
			if ("project".equals(line.kind()) && printable(line)) {
				byProject.computeIfAbsent(line.sourceId(), ignored -> new ArrayList<>()).add(line);
			}
		}
		List<RankedProject> projects = new ArrayList<>();
		byProject.forEach((id, own) -> {
			List<Gain> picked = greedy(own, coverage, new double[coverage.size()], quality);
			double share = picked.stream().mapToDouble(Gain::value).sum();
			if (share <= EPSILON) {
				return;
			}
			List<String> keys = new ArrayList<>(picked.stream().map(gain -> gain.line().key()).toList());
			own.stream()
				.filter(line -> !keys.contains(line.key()) && alone(line, coverage) > EPSILON)
				.sorted(Comparator.comparingDouble((EvidenceLine line) -> -alone(line, coverage))
					.thenComparing(line -> -quality.get(line.key())))
				.forEach(line -> keys.add(line.key()));
			projects.add(new RankedProject(id, library.projectName(id), round(share), List.copyOf(keys)));
		});
		projects.sort(Comparator.comparingDouble((RankedProject project) -> -project.share())
			.thenComparing(project -> -project.keys().size())
			.thenComparing(project -> project.name().toLowerCase(Locale.ROOT)));
		return List.copyOf(projects);
	}

	private static double alone(EvidenceLine line, Coverage coverage) {
		return coverage.gain(line.key(), new double[coverage.size()]);
	}

	/**
	 * Lines not confirmed yet that would cover more of the posting, most first. Then the ones that would put a project
	 * on the resume: the best lines of the projects that would show the most if all their lines were confirmed.
	 */
	private static List<RankedLine> toConfirm(EvidenceIndex index, Coverage coverage, double[] covered,
			Map<String, Double> quality, Library library) {
		List<EvidenceLine> lines = index.lines()
			.stream()
			.filter(line -> !line.heading() && printable(line))
			.toList();
		Map<String, RankedLine> listed = new LinkedHashMap<>();
		lines.stream()
			.filter(line -> !line.confirmed())
			.map(line -> ranked(line, coverage.gain(line.key(), covered), quality, coverage))
			.filter(line -> line.gain() > EPSILON)
			.sorted(Comparator.comparingDouble(RankedLine::gain).reversed())
			.forEach(line -> listed.put(line.key(), line));
		Map<String, EvidenceLine> byKey = new HashMap<>();
		lines.forEach(line -> byKey.put(line.key(), line));
		projects(lines, coverage, quality, library).stream()
			.limit(ResumePlan.PROJECTS)
			.flatMap(project -> project.keys().stream().limit(ResumePlan.PROJECT_LINES))
			.map(byKey::get)
			.filter(line -> !line.confirmed() && !listed.containsKey(line.key()))
			.forEach(line -> listed.put(line.key(), ranked(line, coverage.gain(line.key(), covered), quality,
					coverage)));
		return listed.values().stream().limit(MAX_TO_CONFIRM).toList();
	}

	private static RankedLine ranked(EvidenceLine line, double gain, Map<String, Double> quality, Coverage coverage) {
		return new RankedLine(line.key(), line.kind(), line.sourceId(), line.label(), line.text(), line.confirmed(),
				line.whole(), round(gain), quality.get(line.key()), coverage.backs(line.key()));
	}

	/** Kept skills only. The posting's spelling is used only when it names the same product. */
	private static List<ResumeSkill> skills(Library library, StackDetector detector, String posting) {
		List<StackDetector.Mention> mentions = detector.mentions(posting);
		Map<String, Integer> positions = new HashMap<>();
		List<ResumeSkill> skills = new ArrayList<>();
		for (SkillItem item : SkillService.build(library, detector)) {
			if (!"kept".equals(item.status())) {
				continue;
			}
			Optional<StackDetector.Mention> asked = mentions.stream()
				.filter(mention -> mention.name().equalsIgnoreCase(item.name())
						&& SkillSpelling.same(item.name(), mention.text()))
				.findFirst();
			asked.ifPresent(mention -> positions.put(item.name(), mention.start()));
			skills.add(new ResumeSkill(item.name(), item.category(),
					asked.map(mention -> SkillSpelling.shown(item.name(), mention.text())).orElse(item.name()),
					asked.isPresent(), item.places()));
		}
		skills.sort(Comparator.comparing((ResumeSkill skill) -> !skill.asked())
			.thenComparingInt(skill -> positions.getOrDefault(skill.name(), Integer.MAX_VALUE))
			.thenComparing(Comparator.comparingInt(ResumeSkill::places).reversed())
			.thenComparing(skill -> skill.name().toLowerCase(Locale.ROOT)));
		return List.copyOf(skills);
	}

	private static String sources(Library library, EvidenceIndex index, List<ResumeSkill> skills, MatchResult match) {
		List<String> parts = new ArrayList<>();
		ProfileResponse profile = library.profile();
		parts.add(String.join("|", "profile", profile.displayName(), profile.email(), profile.phone(),
				profile.location(), profile.website(), profile.linkedin()));
		for (ExperienceText job : library.jobs()) {
			parts.add(String.join("|", "job", job.id().toString(), job.title(), job.employer(), job.location(),
					String.valueOf(job.startDate()), String.valueOf(job.endDate()), String.valueOf(job.currentRole())));
		}
		for (EducationResponse school : library.schools()) {
			parts.add(String.join("|", "school", school.id().toString(), school.institution(), school.credential(),
					school.fieldOfStudy(), String.valueOf(school.startDate()), String.valueOf(school.endDate())));
		}
		library.projectNames().forEach((id, name) -> parts.add("project|" + id + "|" + name));
		for (EvidenceLine line : index.lines()) {
			parts.add(line.key() + "|" + line.confirmed());
		}
		for (ResumeSkill skill : skills) {
			parts.add("skill|" + skill.name() + "|" + skill.shown());
		}
		for (RequirementMatch item : match.requirements()) {
			parts.add("answer|" + item.id() + "|" + item.answer());
		}
		parts.sort(Comparator.naturalOrder());
		return EvidenceIndex.hash(parts);
	}

	private static double round(double value) {
		return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
	}

	/**
	 * What the posting asks, as slots: each scored part of an "every part" item, and each "any one" item as a whole.
	 * A slot is worth its share of its group's weight, so a line's gain is the share of the score it could add.
	 */
	private static final class Coverage {

		private record Cover(int slot, double value) {
		}

		private final List<Double> worth = new ArrayList<>();

		private final Map<String, List<Cover>> covers = new HashMap<>();

		private final Map<String, List<LineBacking>> backs = new HashMap<>();

		static Coverage of(MatchResult match) {
			Coverage coverage = new Coverage();
			Map<String, Double> groupWeights = new HashMap<>();
			for (RequirementMatch item : match.requirements()) {
				if (item.weight() != null) {
					groupWeights.merge(item.group(), item.weight(), Double::sum);
				}
			}
			double total = groupWeights.keySet()
				.stream()
				.mapToDouble(group -> GROUP_WEIGHTS.getOrDefault(group, 0))
				.sum();
			if (total <= 0) {
				return coverage;
			}
			for (RequirementMatch item : match.requirements()) {
				if (item.weight() == null || item.answer() != null) {
					continue;
				}
				double scale = GROUP_WEIGHTS.getOrDefault(item.group(), 0) / groupWeights.get(item.group()) / total;
				int itemSlot = "any".equals(item.logic()) ? coverage.slot(scale * item.weight()) : -1;
				for (PartMatch part : item.parts()) {
					double value = "direct".equals(part.support()) ? MatchScorer.DIRECT
							: "transferable".equals(part.support()) ? MatchScorer.TRANSFERABLE : 0;
					int slot = itemSlot >= 0 ? itemSlot : part.weight() == null ? -1 : coverage.slot(scale * part.weight());
					if (value <= 0 || slot < 0) {
						continue;
					}
					for (EvidenceCite cite : part.evidence()) {
						String key = EvidenceLine.key(cite.kind(), cite.id(), cite.line());
						coverage.covers.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new Cover(slot, value));
						coverage.backs.computeIfAbsent(key, ignored -> new ArrayList<>())
							.add(new LineBacking(item.skill(), part.text(), item.group(), part.support()));
					}
				}
			}
			return coverage;
		}

		int size() {
			return worth.size();
		}

		private int slot(double value) {
			worth.add(value);
			return worth.size() - 1;
		}

		double gain(String key, double[] covered) {
			Map<Integer, Double> best = new HashMap<>();
			for (Cover cover : covers.getOrDefault(key, List.of())) {
				best.merge(cover.slot(), cover.value(), Math::max);
			}
			double gain = 0;
			for (Map.Entry<Integer, Double> entry : best.entrySet()) {
				gain += worth.get(entry.getKey()) * Math.max(0, entry.getValue() - covered[entry.getKey()]);
			}
			return gain;
		}

		void apply(String key, double[] covered) {
			for (Cover cover : covers.getOrDefault(key, List.of())) {
				covered[cover.slot()] = Math.max(covered[cover.slot()], cover.value());
			}
		}

		List<LineBacking> backs(String key) {
			return List.copyOf(backs.getOrDefault(key, List.of()));
		}

	}

}
