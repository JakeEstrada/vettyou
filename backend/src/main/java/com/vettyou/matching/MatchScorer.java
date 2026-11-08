package com.vettyou.matching;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vettyou.evidence.StackDetector;

/**
 * Turns judged parts into the Vettyou estimate (ADR 0004). Deterministic: the same parts and lines always give the same
 * numbers, and every number in the result can be recomputed from the ones next to it.
 */
final class MatchScorer {

	static final String LABEL = "Vettyou estimate";

	static final String NOTE = "Our own estimate of how well your saved evidence covers this posting. It is not an "
			+ "employer's ATS score; every company sets theirs up differently.";

	static final String RESUME_NOTE = "Our own estimate of how well this resume covers the posting. It is not an "
			+ "employer's ATS score; every company sets theirs up differently.";

	static final int REQUIRED_WEIGHT = 50;

	static final int RESPONSIBILITY_WEIGHT = 15;

	static final int PREFERRED_WEIGHT = 15;

	static final int STRENGTH_WEIGHT = 15;

	static final int READABILITY_WEIGHT = 15;

	static final double DIRECT = 1;

	static final double TRANSFERABLE = 0.5;

	private static final List<String> GROUPS = List.of("required", "responsibility", "preferred");

	private static final String COVERAGE_HOW = "Average credit per item: direct 1, transferable 0.5, not shown 0. "
			+ "\"Any one\" items take their best part; others average their parts. Parts that repeat an idea from "
			+ "another item share one part's weight. Items that need your answer are left out until you answer.";

	private static final String STRENGTH_HOW = "Average over credited items of the best cited line: 0.4, +0.2 if "
			+ "specific, +0.2 if it shows your own action, +0.2 if it shows a measured result; more independent "
			+ "sources add up to 0.1. Lines about the same project are one source. Items without credit are not "
			+ "counted here again, and school records and a resume's skills section are not rated. Its weight is 15 "
			+ "times your coverage (the "
			+ "groups above, combined), so well-written lines cannot make up for requirements you do not meet.";

	private static final String READABILITY_HOW = "Only scored on a tailored resume. This page scores your saved "
			+ "evidence, not a resume.";

	private static final String RESUME_READABILITY_HOW = "Share of the checks the resume's PDF passes when its text is "
			+ "read back: the text can be read, your name comes first, email and phone are on it, standard headings in "
			+ "order, every line reads back in order, it fits on one page, every character prints. Its weight is 15 "
			+ "times your coverage, so a clean layout cannot make up for requirements you do not meet.";

	private static final List<String> SCALED = List.of("strength", "readability");

	private final StackDetector detector;

	MatchScorer(StackDetector detector) {
		this.detector = detector;
	}

	MatchResult score(List<RequirementInput> requirements, EvidenceIndex index, Library library,
			boolean evidenceChanged) {
		return score(requirements, index, null, library, evidenceChanged, null);
	}

	/**
	 * Scores what one resume shows. {@code shown} holds only the resume's lines; {@code all} is every saved line, so a
	 * part backed by evidence the resume left off reads "hidden" rather than "changed". {@code readability} is the
	 * share of read-back checks the resume passed.
	 */
	MatchResult scoreResume(List<RequirementInput> requirements, EvidenceIndex shown, EvidenceIndex all,
			Library library, double readability) {
		return score(requirements, shown, all, library, false, readability);
	}

	private MatchResult score(List<RequirementInput> requirements, EvidenceIndex index, EvidenceIndex all,
			Library library, boolean evidenceChanged, Double readability) {
		List<Item> items = requirements.stream().map(requirement -> item(requirement, index, all)).toList();
		for (String group : GROUPS) {
			share(items.stream().filter(item -> item.group.equals(group)).toList());
		}
		items.forEach(this::settle);

		List<ScoreFactor> coverage = List.of(
				group(items, "required", "Required qualifications", REQUIRED_WEIGHT, COVERAGE_HOW),
				group(items, "responsibility", "Responsibilities", RESPONSIBILITY_WEIGHT,
						"Same rule, for the duties the job lists. They count less than required qualifications."),
				group(items, "preferred", "Nice to have", PREFERRED_WEIGHT,
						"Same rule, for items the posting marks preferred."));
		Double strength = strength(items, false);
		Double strengthIfConfirmed = strength(items, true);
		List<ScoreFactor> factors = new ArrayList<>(coverage);
		factors.add(new ScoreFactor("strength", "Evidence strength", STRENGTH_WEIGHT,
				strength == null ? null : scaledWeight(coverage, false),
				strengthIfConfirmed == null ? null : scaledWeight(coverage, true), strength, strengthIfConfirmed,
				STRENGTH_HOW));
		if (readability == null) {
			factors.add(new ScoreFactor("readability", "Readability", READABILITY_WEIGHT, null, null, null, null,
					READABILITY_HOW));
		}
		else {
			double value = fixed(readability);
			factors.add(new ScoreFactor("readability", "Readability", READABILITY_WEIGHT,
					scaledWeight(coverage, false), scaledWeight(coverage, true), value, value,
					RESUME_READABILITY_HOW));
		}
		Double unrounded = total(factors, false);
		Double unroundedIfConfirmed = total(factors, true);
		Integer rounded = unrounded == null ? null : (int) Math.round(unrounded);
		Integer roundedIfConfirmed = unroundedIfConfirmed == null ? null : (int) Math.round(unroundedIfConfirmed);
		int toConfirm = (int) items.stream()
			.filter(item -> item.input.answer() == null)
			.flatMap(item -> item.parts.stream())
			.filter(part -> "unconfirmed".equals(part.status))
			.count();
		int toAnswer = (int) items.stream().filter(item -> item.canMarkMet() && item.input.answer() == null).count();
		MatchScore score = new MatchScore(rounded, unrounded, roundedIfConfirmed, unroundedIfConfirmed, factors,
				formula(factors, false, unrounded, rounded),
				formula(factors, true, unroundedIfConfirmed, roundedIfConfirmed), toConfirm, toAnswer,
				readiness(library), LABEL, readability == null ? NOTE : RESUME_NOTE);
		return new MatchResult(items.stream().map(MatchScorer::output).toList(), score, evidenceChanged);
	}

	private Item item(RequirementInput requirement, EvidenceIndex index, EvidenceIndex all) {
		Item item = new Item(requirement, group(requirement),
				requirement.parts().stream().map(part -> part(part, index, all)).toList());
		if ("gap".equals(requirement.answer()) || "meets".equals(requirement.answer())) {
			double value = "meets".equals(requirement.answer()) ? 1 : 0;
			item.single(value, value);
			return item;
		}
		List<Part> assessed = item.parts.stream().filter(part -> part.now != null).toList();
		if (assessed.isEmpty()) {
			return item;
		}
		if ("any".equals(requirement.logic())) {
			double bestNow = assessed.stream().mapToDouble(part -> part.now).max().orElse(0);
			double bestIfConfirmed = assessed.stream().mapToDouble(part -> part.ifConfirmed).max().orElse(0);
			if (assessed.size() < item.parts.size() && bestIfConfirmed < 1) {
				return item;
			}
			item.single(bestNow, bestIfConfirmed);
			return item;
		}
		for (Part part : assessed) {
			part.unit = true;
			part.mass = 1.0 / assessed.size();
		}
		item.scored = true;
		return item;
	}

	private static Part part(PartInput input, EvidenceIndex index, EvidenceIndex all) {
		Map<String, Cite> cites = new LinkedHashMap<>();
		for (EvidenceRef ref : input.evidence()) {
			index.holding(ref).ifPresent(line -> cites.putIfAbsent(line.ref(), new Cite(line, ref.excerpt())));
		}
		List<Cite> found = List.copyOf(cites.values());
		String support = input.support() == null ? "none" : input.support();
		return switch (support) {
			case "unclear" -> new Part(input, "unclear", null, null, found);
			case "direct", "transferable" -> {
				double value = "direct".equals(support) ? DIRECT : TRANSFERABLE;
				if (found.stream().anyMatch(cite -> cite.line().confirmed())) {
					yield new Part(input, support, value, value, found);
				}
				if (!found.isEmpty()) {
					yield new Part(input, "unconfirmed", 0.0, value, found);
				}
				List<EvidenceLine> saved = all == null ? List.of()
						: input.evidence().stream().map(all::holding).flatMap(Optional::stream).toList();
				if (saved.stream().anyMatch(EvidenceLine::confirmed)) {
					yield new Part(input, "hidden", 0.0, 0.0, List.of());
				}
				yield new Part(input, saved.isEmpty() ? "changed" : "unconfirmed", 0.0, 0.0, List.of());
			}
			default -> new Part(input, "not_shown", 0.0, 0.0, List.of());
		};
	}

	/** Inside one group, items that ask for the same concept split one item's worth of weight for it. */
	private static void share(List<Item> items) {
		Map<String, Map<Item, Double>> byConcept = new LinkedHashMap<>();
		for (Item item : items) {
			for (Part part : item.parts) {
				if (part.unit) {
					byConcept.computeIfAbsent(part.concept(), key -> new LinkedHashMap<>())
						.merge(item, part.mass, Double::sum);
				}
			}
		}
		for (Map.Entry<String, Map<Item, Double>> concept : byConcept.entrySet()) {
			Map<Item, Double> masses = concept.getValue();
			if (masses.size() < 2) {
				continue;
			}
			double largest = masses.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
			double sum = masses.values().stream().mapToDouble(Double::doubleValue).sum();
			for (Item item : masses.keySet()) {
				for (Part part : item.parts) {
					if (part.unit && part.concept().equals(concept.getKey())) {
						part.mass *= largest / sum;
						part.sharedBy = masses.size();
					}
				}
			}
		}
	}

	private void settle(Item item) {
		if (item.scored) {
			if (item.single) {
				item.weight = 1.0;
				item.credit = item.singleNow;
				item.creditIfConfirmed = item.singleIfConfirmed;
			}
			else {
				List<Part> units = item.parts.stream().filter(part -> part.unit).toList();
				double weight = units.stream().mapToDouble(part -> part.mass).sum();
				item.weight = weight;
				item.credit = units.stream().mapToDouble(part -> part.mass * part.now).sum() / weight;
				item.creditIfConfirmed = units.stream().mapToDouble(part -> part.mass * part.ifConfirmed).sum() / weight;
			}
			if (item.input.answer() == null) {
				item.strength = item.credit > 0 ? quality(cites(item, false)) : null;
				item.strengthIfConfirmed = item.creditIfConfirmed > 0 ? quality(cites(item, true)) : null;
			}
		}
		item.status = status(item);
	}

	private static String status(Item item) {
		if ("gap".equals(item.input.answer())) {
			return "gap";
		}
		if ("meets".equals(item.input.answer())) {
			return "met";
		}
		if (!item.scored) {
			return "unclear";
		}
		if (item.credit >= 0.999) {
			return "direct";
		}
		if (item.credit > 0) {
			return "partial";
		}
		if (item.creditIfConfirmed > 0) {
			return "unconfirmed";
		}
		if (item.parts.stream().anyMatch(part -> "hidden".equals(part.status))) {
			return "hidden";
		}
		return item.parts.stream().anyMatch(part -> "unconfirmed".equals(part.status)) ? "unconfirmed" : "not_shown";
	}

	private static List<Cite> cites(Item item, boolean ifConfirmed) {
		List<Cite> cites = new ArrayList<>();
		for (Part part : item.parts) {
			Double value = ifConfirmed ? part.ifConfirmed : part.now;
			if (value != null && value > 0) {
				part.cites.stream().filter(cite -> ifConfirmed || cite.line().confirmed()).forEach(cites::add);
			}
		}
		return cites;
	}

	/**
	 * Null when only school records or a resume's skills section back the item: a degree or a list of skills is a
	 * record, not a description of work to rate.
	 */
	private Double quality(List<Cite> cites) {
		List<Cite> work = cites.stream()
			.filter(cite -> !"school".equals(cite.line().kind()) && !"skills".equals(cite.line().kind()))
			.toList();
		if (work.isEmpty()) {
			return null;
		}
		double best = work.stream()
			.mapToDouble(cite -> EvidenceQuality.line(cite.line().text(), detector))
			.max()
			.orElse(0);
		int sources = (int) work.stream().map(cite -> cite.line().group()).distinct().count();
		return Math.min(1, best + EvidenceQuality.corroboration(sources));
	}

	private static Double coverage(List<Item> items, String group, boolean ifConfirmed) {
		List<Item> scored = items.stream().filter(item -> item.group.equals(group) && item.weight != null).toList();
		double weights = scored.stream().mapToDouble(item -> item.weight).sum();
		if (weights <= 0) {
			return null;
		}
		return fixed(scored.stream()
			.mapToDouble(item -> item.weight * (ifConfirmed ? item.creditIfConfirmed : item.credit))
			.sum() / weights);
	}

	private static Double strength(List<Item> items, boolean ifConfirmed) {
		List<Double> values = items.stream()
			.map(item -> ifConfirmed ? item.strengthIfConfirmed : item.strength)
			.filter(Objects::nonNull)
			.toList();
		return values.isEmpty() ? null : fixed(values.stream().mapToDouble(Double::doubleValue).average().orElse(0));
	}

	/** Factor values carry four decimals, so the formula shows exactly the numbers the total is built from. */
	private static double fixed(double value) {
		return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
	}

	private static ScoreFactor group(List<Item> items, String key, String label, int weight, String how) {
		Double value = coverage(items, key, false);
		Double valueIfConfirmed = coverage(items, key, true);
		return new ScoreFactor(key, label, weight, value == null ? null : (double) weight,
				valueIfConfirmed == null ? null : (double) weight, value, valueIfConfirmed, how);
	}

	/** The coverage groups combined, to four decimals: what the score would be without evidence strength. */
	private static BigDecimal coverageShare(List<ScoreFactor> coverage, boolean ifConfirmed) {
		BigDecimal weighted = BigDecimal.ZERO;
		int weights = 0;
		for (ScoreFactor factor : coverage) {
			Double value = value(factor, ifConfirmed);
			if (value != null) {
				weighted = weighted.add(BigDecimal.valueOf(factor.weight()).multiply(BigDecimal.valueOf(value)));
				weights += factor.weight();
			}
		}
		return weights == 0 ? BigDecimal.ZERO : weighted.divide(BigDecimal.valueOf(weights), 4, RoundingMode.HALF_UP);
	}

	/**
	 * Evidence strength rates the lines behind the credit, and readability rates the resume that shows them, so both
	 * weigh in proportion to the credit: with no coverage they add nothing, and missing items still drop out of
	 * strength rather than counting against it.
	 */
	private static double scaledWeight(List<ScoreFactor> coverage, boolean ifConfirmed) {
		return coverageShare(coverage, ifConfirmed).multiply(BigDecimal.valueOf(STRENGTH_WEIGHT)).doubleValue();
	}

	/** Null until at least one coverage group can be scored. Left-out factors drop out of the divisor. */
	private static Double total(List<ScoreFactor> factors, boolean ifConfirmed) {
		boolean coverage = factors.stream()
			.filter(factor -> GROUPS.contains(factor.key()))
			.anyMatch(factor -> value(factor, ifConfirmed) != null);
		if (!coverage) {
			return null;
		}
		BigDecimal weighted = BigDecimal.ZERO;
		BigDecimal weights = BigDecimal.ZERO;
		for (ScoreFactor factor : factors) {
			Double value = value(factor, ifConfirmed);
			Double weight = activeWeight(factor, ifConfirmed);
			if (value != null && weight != null) {
				weighted = weighted.add(BigDecimal.valueOf(weight).multiply(BigDecimal.valueOf(value)));
				weights = weights.add(BigDecimal.valueOf(weight));
			}
		}
		return weighted.multiply(BigDecimal.valueOf(100)).divide(weights, 10, RoundingMode.HALF_UP).doubleValue();
	}

	static String formula(List<ScoreFactor> factors, boolean ifConfirmed, Double unrounded, Integer total) {
		if (unrounded == null) {
			return "Nothing can be scored yet.";
		}
		List<String> terms = new ArrayList<>();
		BigDecimal weights = BigDecimal.ZERO;
		StringBuilder scaled = new StringBuilder();
		for (ScoreFactor factor : factors) {
			Double value = value(factor, ifConfirmed);
			Double weight = activeWeight(factor, ifConfirmed);
			if (value == null || weight == null) {
				continue;
			}
			terms.add(plain(BigDecimal.valueOf(weight)) + String.format(Locale.ROOT, " x %.4f", value));
			weights = weights.add(BigDecimal.valueOf(weight));
			if (SCALED.contains(factor.key())) {
				BigDecimal share = BigDecimal.valueOf(weight)
					.divide(BigDecimal.valueOf(factor.weight()), 4, RoundingMode.HALF_UP);
				scaled.append(String.format(Locale.ROOT, " %s weight: %d x %s coverage = %s.", factor.label(),
						factor.weight(), share.toPlainString(), plain(BigDecimal.valueOf(weight))));
			}
		}
		return String.format(Locale.ROOT, "100 x (%s) / %s = %s, rounded to %d.%s", String.join(" + ", terms),
				plain(weights), BigDecimal.valueOf(unrounded).setScale(2, RoundingMode.HALF_UP).toPlainString(), total,
				scaled);
	}

	private static String plain(BigDecimal number) {
		return number.signum() == 0 ? "0" : number.stripTrailingZeros().toPlainString();
	}

	private static Double value(ScoreFactor factor, boolean ifConfirmed) {
		return ifConfirmed ? factor.valueIfConfirmed() : factor.value();
	}

	private static Double activeWeight(ScoreFactor factor, boolean ifConfirmed) {
		return ifConfirmed ? factor.activeWeightIfConfirmed() : factor.activeWeight();
	}

	private static Readiness readiness(Library library) {
		List<Readiness.Check> checks = List.of(
				new Readiness.Check("Name", !library.profile().displayName().isBlank()),
				new Readiness.Check("Email", !library.profile().email().isBlank()),
				new Readiness.Check("Phone", !library.profile().phone().isBlank()),
				new Readiness.Check("Location", !library.profile().location().isBlank()),
				new Readiness.Check("At least one job", !library.jobs().isEmpty()),
				new Readiness.Check("Job dates", library.jobs().stream().anyMatch(job -> job.startDate() != null)),
				new Readiness.Check("Education", !library.schools().isEmpty()));
		long done = checks.stream().filter(Readiness.Check::done).count();
		return new Readiness((double) done / checks.size(), checks);
	}

	private static String group(RequirementInput requirement) {
		if ("preferred".equals(requirement.importance())) {
			return "preferred";
		}
		return "responsibility".equals(requirement.kind()) ? "responsibility" : "required";
	}

	private static RequirementMatch output(Item item) {
		RequirementInput input = item.input;
		List<PartMatch> parts = item.parts.stream()
			.map(part -> new PartMatch(part.input.text(), part.concept(),
					part.input.support() == null ? "none" : part.input.support(), part.status,
					part.input.reason() == null ? "" : part.input.reason(), part.unit ? part.mass : null, part.sharedBy,
					part.cites.stream()
						.map(cite -> new EvidenceCite(cite.line().kind(), cite.line().sourceId(), cite.line().label(),
								cite.excerpt(), cite.line().text(), cite.line().confirmed()))
						.toList()))
			.toList();
		return new RequirementMatch(input.id(), input.skill(), input.kind(), input.importance(), item.group,
				input.category(), input.quote(), input.logic(), item.status, note(item), input.answer(),
				input.answeredAt(), item.canMarkMet(), item.weight, item.credit, item.creditIfConfirmed, item.strength,
				parts);
	}

	private static String note(Item item) {
		String prefix = "any".equals(item.input.logic()) && item.parts.size() > 1 ? "One of these is enough. " : "";
		String question = item.canMarkMet() ? " Part of it needs your answer." : "";
		return switch (item.status) {
			case "gap" -> "You said you don't have this.";
			case "met" -> "You said you meet this. No saved line shows it, so a tailored resume will not cite one.";
			case "unclear" -> prefix + "This can't be settled from your saved text, for example years or eligibility. "
					+ "Answer it with the buttons below.";
			case "direct" -> prefix + "Shown directly" + sources(item) + "." + question;
			case "unconfirmed" -> prefix + "Shown only in project lines you have not confirmed yet. Confirm them on "
					+ "the project page to count them." + question;
			case "hidden" -> prefix + "Your saved lines show this, but none of them is on this resume." + question;
			case "partial" -> prefix + summary(item) + (item.creditIfConfirmed > item.credit
					? " More counts once you confirm the flagged project lines." : "");
			default -> prefix + "Not found in your job write-ups, project lines, or education."
					+ (item.parts.stream().anyMatch(part -> "changed".equals(part.status))
							? " A line this relied on was edited or removed; re-check against your evidence." : "")
					+ question;
		};
	}

	private static String sources(Item item) {
		long sources = cites(item, false).stream().map(cite -> cite.line().group()).distinct().count();
		return sources >= 2 ? " in " + sources + " separate pieces of work" : "";
	}

	private static String summary(Item item) {
		Map<String, List<String>> byStatus = item.parts.stream()
			.collect(Collectors.groupingBy(part -> part.status, LinkedHashMap::new,
					Collectors.mapping(part -> part.input.text(), Collectors.toList())));
		List<String> sentences = new ArrayList<>();
		for (String status : List.of("direct", "transferable", "unconfirmed", "hidden", "not_shown", "changed",
				"unclear")) {
			List<String> texts = byStatus.get(status);
			if (texts != null) {
				sentences.add(MatchLabels.part(status) + ": " + String.join("; ", texts) + ".");
			}
		}
		return String.join(" ", sentences);
	}

	private record Cite(EvidenceLine line, String excerpt) {
	}

	private static final class Part {

		private final PartInput input;

		private final String status;

		private final Double now;

		private final Double ifConfirmed;

		private final List<Cite> cites;

		private boolean unit;

		private double mass;

		private int sharedBy = 1;

		private Part(PartInput input, String status, Double now, Double ifConfirmed, List<Cite> cites) {
			this.input = input;
			this.status = status;
			this.now = now;
			this.ifConfirmed = ifConfirmed;
			this.cites = cites;
		}

		private String concept() {
			String concept = input.concept() == null || input.concept().isBlank() ? input.text() : input.concept();
			return Excerpts.normalize(concept);
		}

	}

	private static final class Item {

		private final RequirementInput input;

		private final String group;

		private final List<Part> parts;

		private boolean scored;

		private boolean single;

		private double singleNow;

		private double singleIfConfirmed;

		private Double weight;

		private Double credit;

		private Double creditIfConfirmed;

		private Double strength;

		private Double strengthIfConfirmed;

		private String status;

		private Item(RequirementInput input, String group, List<Part> parts) {
			this.input = input;
			this.group = group;
			this.parts = parts;
		}

		private void single(double now, double ifConfirmed) {
			scored = true;
			single = true;
			singleNow = now;
			singleIfConfirmed = ifConfirmed;
		}

		private boolean canMarkMet() {
			return parts.stream().anyMatch(part -> "unclear".equals(part.status));
		}

	}

}
