package com.vettyou.templates;

import java.util.List;

/**
 * What a resume says, with no layout. A template decides only how it looks: it never adds, drops, or rewords text.
 */
public record ResumeDocument(String name, List<String> contact, List<Section> sections) {

	public record Section(String heading, List<Row> rows, List<Entry> entries) {
	}

	/** A labeled line, such as a skills row: "Languages" and "Java, Python". */
	public record Row(String label, String text) {
	}

	/** A job, project, or school: a title with its dates, a line under it, then bullets. */
	public record Entry(String title, String dates, String subtitle, List<String> bullets) {
	}

}
