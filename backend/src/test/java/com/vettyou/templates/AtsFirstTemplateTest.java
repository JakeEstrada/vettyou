package com.vettyou.templates;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.vettyou.templates.ResumeDocument.Entry;
import com.vettyou.templates.ResumeDocument.Row;
import com.vettyou.templates.ResumeDocument.Section;

class AtsFirstTemplateTest {

	private final AtsFirstTemplate template = new AtsFirstTemplate();

	private static ResumeDocument document(List<String> bullets) {
		return new ResumeDocument("Ada Lovelace", List.of("ada@example.com", "(555) 010-0100", "London"),
				List.of(new Section("Skills", List.of(new Row("Languages", "Java, Python")), List.of()),
						new Section("Experience", List.of(),
								List.of(new Entry("Developer", "Jan 2023 \u2013 Present", "Acme, London", bullets))),
						new Section("Education", List.of(),
								List.of(new Entry("B.S. Computer Science", "", "State University", List.of())))));
	}

	@Test
	void textReadsBackInTheOrderItIsShown() {
		AtsFirstTemplate.Rendered rendered = template
			.render(document(List.of("Built React screens backed by PostgreSQL", "Mentored two junior developers")));

		String text = template.readBack(rendered.pdf());

		assertThat(rendered.pages()).isEqualTo(1);
		assertThat(rendered.unprintable()).isEmpty();
		assertThat(text.lines().findFirst()).hasValue("Ada Lovelace");
		assertThat(text).containsSubsequence("ada@example.com", "SKILLS", "Languages:", "Java, Python", "EXPERIENCE",
				"Developer", "Jan 2023 \u2013 Present", "Acme, London", "Built React screens backed by PostgreSQL",
				"Mentored two junior developers", "EDUCATION", "B.S. Computer Science", "State University");
		assertThat(rendered.fragments()).startsWith("Ada Lovelace");
		assertThat(rendered.fragments()).contains("Built React screens backed by PostgreSQL").doesNotContain("\u2022");
	}

	@Test
	void charactersTheFontLacksAreReportedAndPrintAsQuestionMarks() {
		AtsFirstTemplate.Rendered rendered = template
			.render(document(List.of("Moved billing \u2192 cloud \u2713", "State\u2011of\u2011the\u2011art caching")));

		assertThat(rendered.unprintable()).containsExactly("\u2192", "\u2713");
		assertThat(template.readBack(rendered.pdf())).contains("Moved billing ? cloud ?", "State-of-the-art caching");
	}

	@Test
	void aLongResumeRunsOntoMorePages() {
		List<String> bullets = IntStream.range(0, 80).mapToObj(i -> "Line " + i + " about work that was done").toList();

		assertThat(template.pages(document(bullets))).isGreaterThan(1);
		assertThat(template.render(document(bullets)).pages()).isEqualTo(template.pages(document(bullets)));
	}

	@Test
	void aWordWiderThanTheLineIsSplitWithoutAddingCharacters() {
		String url = "https://example.com/" + "a".repeat(200);

		String text = template.readBack(template.render(document(List.of(url))).pdf());

		assertThat(text.replaceAll("\\s+", "")).contains(url);
	}

	@Test
	void thePreviewIsAPngOfTheFirstPage() {
		byte[] png = template.preview(template.render(document(List.of("Built things"))).pdf());

		assertThat(png).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
	}

	@Test
	void marksSitOnTheScoredBulletOnTheFirstPagePng() {
		ResumeDocument resume = document(
				List.of("Built React screens backed by PostgreSQL", "Mentored two junior developers"));

		List<AtsFirstTemplate.Mark> marks = template.marks(resume,
				List.of("Built React screens backed by PostgreSQL", "A line that is not on the resume"));

		assertThat(marks).hasSize(1);
		assertThat(marks.get(0).text()).isEqualTo("Built React screens backed by PostgreSQL");
		assertThat(marks.get(0).boxes()).isNotEmpty();
		AtsFirstTemplate.Box box = marks.get(0).boxes().get(0);
		assertThat(box.width()).isGreaterThan(80);
		assertThat(box.height()).isGreaterThan(10);
		assertThat(box.x()).isGreaterThan(50);
		assertThat(box.y()).isGreaterThan(80);
		assertThat(box.y() + box.height()).isLessThan(1100);
	}

}
