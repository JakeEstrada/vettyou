package com.vettyou.matching;

import java.util.regex.Pattern;

import com.vettyou.evidence.StackDetector;

/**
 * How convincing a line is, from its wording alone: a base for any cited line, plus specificity, the person's own
 * action, and a measured result. More independent sources add a little, with diminishing returns.
 */
final class EvidenceQuality {

	static final double BASE = 0.4;

	static final double SPECIFIC = 0.2;

	static final double OWN_ACTION = 0.2;

	static final double RESULT = 0.2;

	static final double CORROBORATION = 0.1;

	private static final int DETAILED_WORDS = 10;

	private static final Pattern DIGIT = Pattern.compile("\\d");

	private static final Pattern NUMBER = Pattern.compile("(?i)\\b(?:\\d[\\d,.]*|one|two|three|four|five|six|seven|"
			+ "eight|nine|ten|eleven|twelve|fifteen|twenty|thirty|forty|fifty|hundreds?|thousands?|dozens?|half)\\b");

	private static final Pattern CHANGE = Pattern.compile("(?i)\\b(?:cut|cuts|reduc\\w*|increas\\w*|improv\\w*|"
			+ "sav(?:e|ed|es|ing)|grew|grow\\w*|decreas\\w*|eliminat\\w*|doubl\\w*|tripl\\w*|faster|fewer|lower(?:ed)?|"
			+ "rais(?:e|ed)|boost\\w*|shorten\\w*|halv\\w*)\\b");

	private static final Pattern RATIO = Pattern.compile("(?i)\\d\\s*(?:%|percent\\b|x\\b)");

	private static final Pattern ACTION = Pattern.compile("(?i)^(?:i\\s+)?(?:owned|own|built|build|designed|design|"
			+ "developed|develop|wrote|write|implemented|implement|led|lead|managed|manage|architected|deployed|deploy|"
			+ "created|create|engineered|automated|automate|diagnosed|diagnose|fixed|fix|trained|train|supervised|"
			+ "supervise|coordinated|coordinate|translated|translate|defined|define|identified|identify|ran|run|"
			+ "launched|launch|migrated|migrate|integrated|integrate|tested|test|shipped|ship|delivered|deliver|"
			+ "maintained|maintain|configured|configure|modeled|analyzed|analyze|optimized|optimize|refactored|"
			+ "refactor|mentored|mentor|presented|negotiated|scheduled|estimated|reconciled|established|introduced|"
			+ "replaced|added|set up|use|used|work|worked|point of contact|served|handled|handle|planned|organized|"
			+ "reduced|reduce|improved|improve|cut|split|wired|connected|tracked|track|documented|document|reviewed|"
			+ "review|provided|provide|performed|perform|conducted|conduct|operated|operate|assessed|assess|"
			+ "administered|administer|educated|educate|taught|teach|charted|chart|precepted|precept|treated|treat|"
			+ "monitored|monitor|prepared|prepare|processed|process|supported|support|advised|advise|consulted|"
			+ "consult|installed|install|repaired|repair|inspected|inspect|oversaw|oversee|directed|direct|sold|sell|"
			+ "kept|keep|resolved|resolve|communicated|communicate|investigated|investigate|researched|research)\\b");

	private static final Pattern HEDGE = Pattern.compile(
			"(?i)\\b(?:helped|assisted|contributed|participated|familiar|exposure|learning|coursework|basic knowledge)\\b");

	private EvidenceQuality() {
	}

	/** 0.4 to 1.0 for one line. */
	static double line(String text, StackDetector detector) {
		String line = text.strip();
		boolean specific = DIGIT.matcher(line).find() || line.split("\\s+").length >= DETAILED_WORDS
				|| !detector.detect(line).isEmpty();
		double own = HEDGE.matcher(line).find() ? 0.5 : ACTION.matcher(line).find() ? 1 : 0;
		return BASE + (specific ? SPECIFIC : 0) + OWN_ACTION * own + (measured(line) ? RESULT : 0);
	}

	/** A stated result with a number: a percentage or ratio, or a change word next to a quantity. */
	static boolean measured(String text) {
		return RATIO.matcher(text).find() || CHANGE.matcher(text).find() && NUMBER.matcher(text).find();
	}

	/** Extra credit for independent sources beyond the first: 0.05, 0.075, 0.0875, ... never above 0.1. */
	static double corroboration(int sources) {
		return sources <= 1 ? 0 : CORROBORATION * (1 - Math.pow(0.5, sources - 1));
	}

}
