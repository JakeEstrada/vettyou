package com.vettyou.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import com.vettyou.matching.EvidenceCite;
import com.vettyou.matching.MatchScore;
import com.vettyou.matching.PartMatch;
import com.vettyou.matching.Readiness;
import com.vettyou.matching.RequirementMatch;
import com.vettyou.matching.ScoreFactor;

class JobReportTest {

	private final JobReport report = new JobReport();

	private final RequirementMatch leading = new RequirementMatch(UUID.randomUUID(),
			"Leading technical projects or data workflows", "qualification", "required", "required", "experience",
			"Experience leading technical projects or data workflows.", "any", "direct",
			"One of these is enough. Shown directly.", null, null, false, 1.0, 1.0, 1.0, 0.8,
			List.of(new PartMatch("leading technical projects", "project ownership", "direct", "direct",
					"Owned systems end to end.", null, 1,
					List.of(new EvidenceCite("job", UUID.randomUUID(), "Developer at Liminal, 2025-05 to present",
							"Owned systems from requirements discovery", "Owned systems from requirements discovery "
									+ "through deployment", true))),
					new PartMatch("data workflows", "data workflows", "none", "not_shown", "Nothing shows this.", null, 1,
							List.of())));

	private final RequirementMatch automation = new RequirementMatch(UUID.randomUUID(), "Pipeline automation",
			"responsibility", "required", "responsibility", "practice",
			"Drive automation of pipeline workflows, including task generation 🦀", "all", "partial",
			"Transferable: automation. Not shown: task generation.", null, null, false, 1.0, 0.25, 0.5, 0.6,
			List.of(new PartMatch("automation", "workflow automation", "transferable", "transferable",
					"Business automation, not data pipelines.", 0.5, 2,
					List.of(new EvidenceCite("project", UUID.randomUUID(), "Liminnality",
							"automated stage transitions", "Automated stage transitions", false))),
					new PartMatch("task generation", "task generation", "none", "not_shown", "", 0.5, 1, List.of())));

	private final MatchScore score = new MatchScore(81, 80.6584, 85, 85.3317,
			List.of(new ScoreFactor("required", "Required qualifications", 50, 50.0, 50.0, 1.0, 1.0,
					"Average credit per item."),
					new ScoreFactor("responsibility", "Responsibilities", 15, 15.0, 15.0, 0.25, 0.5, "Same rule."),
					new ScoreFactor("preferred", "Nice to have", 15, null, null, null, null, "Same rule."),
					new ScoreFactor("strength", "Evidence strength", 15, 12.4035, 13.269, 0.7, 0.7, "Best line."),
					new ScoreFactor("readability", "Readability", 15, null, null, null, null, "Left out.")),
			"100 x (50 x 1.0000 + 15 x 0.2500 + 12.4035 x 0.7000) / 77.4035 = 80.66, rounded to 81. Evidence "
					+ "strength weight: 15 x 0.8269 coverage = 12.4035.",
			"100 x (50 x 1.0000 + 15 x 0.5000 + 13.269 x 0.7000) / 78.269 = 85.33, rounded to 85. Evidence strength "
					+ "weight: 15 x 0.8846 coverage = 13.269.", 1, 0,
			new Readiness(5.0 / 6, List.of(new Readiness.Check("Name", true), new Readiness.Check("Phone", false))),
			"Vettyou estimate", "Not an employer's ATS score.");

	private final JobPostingResponse posting = new JobPostingResponse(UUID.randomUUID(),
			"Strategic Project Lead, Coding/SWE", "Crossing Hurdles", "",
			"Position: Strategic Project Lead Drive automation of pipeline workflows, including task generation 🦀",
			"ready", "openai:test-model", "openai:test-model",
			Instant.parse("2026-11-07T17:05:00Z"), true, "",
			"Position: Strategic Project Lead\n\tDrive automation of pipeline workflows, including task generation 🦀",
			Instant.parse("2026-11-07T17:00:00Z"), List.of(leading, automation), score);

	@Test
	void textShowsTheFormulaEveryPartAndWhereEachQuoteCameFrom() {
		String text = report.text(posting);

		assertThat(text).contains("# Vettyou job match report", "Strategic Project Lead, Coding/SWE - Crossing Hurdles",
				"Your saved evidence changed after this check.",
				"## Score: 81 / 100 (Vettyou estimate, confirmed evidence only)",
				"If you confirm the 1 flagged project line: 85 / 100.",
				"Nice to have: n/a, left out (weight 15).",
				"Responsibilities: 0.2500 now, 0.5000 if confirmed; weight 15; 3.7500 points now.",
				"Evidence strength: 0.7000 now, 0.7000 if confirmed; weight 15 x coverage = 12.4035 now, 13.2690 if "
						+ "confirmed;",
				"Formula now: 100 x (50 x 1.0000 + 15 x 0.2500 + 12.4035 x 0.7000) / 77.4035 = 80.66",
				"## Application readiness (not part of the score)", "Phone missing",
				"## Required qualifications (1)", "any one part is enough", "Status: Direct evidence.",
				"Part: data workflows - Not shown. Nothing shows this.",
				"Evidence (job, Developer at Liminal, 2025-05 to present): \"Owned systems from requirements discovery\"",
				"## Responsibilities (1)", "Status: Transferable or partial evidence. Weight 1.000, credit 0.250 (if "
						+ "confirmed 0.500), strength 0.600.",
				"Part: automation - Transferable (weight 0.500), same idea in 2 items.",
				"Evidence (project, Liminnality, not confirmed yet): \"automated stage transitions\"",
				"Group value: 0.2500 / 1.0000 = 0.2500 now; 0.5000 / 1.0000 = 0.5000 if confirmed.",
				"## The posting as pasted", "task generation 🦀")
			.doesNotContain("ATS score:", "\n\n\n");
	}

	@Test
	void pdfDrawsCharactersHelveticaLacksAsQuestionMarks() throws Exception {
		byte[] pdf = report.pdf(posting);

		try (PDDocument document = Loader.loadPDF(pdf)) {
			String text = new PDFTextStripper().getText(document);
			assertThat(text).contains("Vettyou job match report", "Evidence (project, Liminnality, not confirmed yet)",
					"task generation ?");
		}
	}

}
