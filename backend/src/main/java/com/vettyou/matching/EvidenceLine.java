package com.vettyou.matching;

import java.util.UUID;

/**
 * One numbered line of the person's own data. {@code confirmed} is false for project lines not kept yet.
 * {@code group} ties together lines about the same piece of work, so one project counts as one source.
 * {@code heading} marks job titles and schools, which every resume prints; {@code whole} is false when the line was
 * cut to the length limit.
 */
record EvidenceLine(String ref, String kind, UUID sourceId, String label, String text, boolean confirmed,
		String group, boolean heading, boolean whole) {

	/** The same line whatever its number: what it belongs to and its words. */
	String key() {
		return key(kind, sourceId, text);
	}

	static String key(String kind, UUID sourceId, String text) {
		return kind + "|" + sourceId + "|" + Excerpts.normalize(text);
	}

}
