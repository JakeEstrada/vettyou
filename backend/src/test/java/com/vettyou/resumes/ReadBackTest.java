package com.vettyou.resumes;

import static com.vettyou.resumes.ResumeFitterTest.ADA;
import static com.vettyou.resumes.ResumeFitterTest.CURRENT;
import static com.vettyou.resumes.ResumeFitterTest.line;
import static com.vettyou.resumes.ResumeFitterTest.plan;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.vettyou.matching.MatchResult;
import com.vettyou.matching.RankedLine;
import com.vettyou.matching.ResumePlan;
import com.vettyou.matching.ResumeSkill;
import com.vettyou.profile.ExperienceText;
import com.vettyou.profile.ProfileResponse;
import com.vettyou.templates.AtsFirstTemplate;

class ReadBackTest {

	private final AtsFirstTemplate template = new AtsFirstTemplate();

	private final ResumeFitter fitter = new ResumeFitter(template);

	private Map<String, ReadCheck> checks(ResumePlan plan) {
		ResumeFitter.Fit fit = fitter.fit(plan);
		AtsFirstTemplate.Rendered rendered = template.render(fit.document());
		return ReadBack.checks(fit.document(), plan.profile(), rendered, template.readBack(rendered.pdf()))
			.stream()
			.collect(Collectors.toMap(ReadCheck::key, Function.identity()));
	}

	@Test
	void aFittedResumePassesEveryCheck() {
		Map<String, ReadCheck> checks = checks(plan(ADA,
				List.of(line("job", CURRENT, "Built React screens backed by PostgreSQL", 0.5),
						line("job", CURRENT, "Mentored two junior developers", 0.2)),
				List.of(new ResumeSkill("React", "framework", "React", true, 1))));

		assertThat(checks).hasSize(8);
		assertThat(checks.values()).allSatisfy(check -> {
			assertThat(check.passed()).as(check.label() + ": " + check.detail()).isTrue();
			assertThat(check.detail()).isEmpty();
		});
	}

	@Test
	void missingContactDetailsFailOnlyTheirOwnChecks() {
		Map<String, ReadCheck> checks = checks(
				plan(new ProfileResponse("Ada Lovelace", "", "", "London", "", ""), List.of(), List.of()));

		assertThat(checks.get("email").passed()).isFalse();
		assertThat(checks.get("email").detail()).isEqualTo("Add the email you apply with on the profile page.");
		assertThat(checks.get("phone").passed()).isFalse();
		assertThat(checks.values().stream().filter(ReadCheck::passed).count()).isEqualTo(6);
	}

	@Test
	void anUnprintableCharacterFailsOneCheckButTheTextStillReadsBackInOrder() {
		Map<String, ReadCheck> checks = checks(
				plan(ADA, List.of(line("job", CURRENT, "Moved billing \u2192 cloud", 0.5)), List.of()));

		assertThat(checks.get("characters").passed()).isFalse();
		assertThat(checks.get("characters").detail()).contains("\u2192");
		assertThat(checks.get("order").passed()).isTrue();
	}

	@Test
	void aResumeWhoseJobsAloneRunOverFailsTheOnePageCheck() {
		ResumePlan base = plan(ADA, List.of(), List.of());
		List<ExperienceText> jobs = IntStream.range(0, 35)
			.mapToObj(i -> ResumeFitterTest.job(UUID.randomUUID(), "Role " + i, "Employer " + i, null, null, false))
			.toList();
		ResumePlan crowded = new ResumePlan(new MatchResult(List.of(), null, false), List.<RankedLine>of(), List.of(),
				List.of(), List.of(), "sources", ADA, jobs, base.schools(), base.projectNames());

		Map<String, ReadCheck> checks = checks(crowded);

		assertThat(checks.get("pages").passed()).isFalse();
		assertThat(checks.get("pages").detail()).startsWith("It runs to 2 pages");
		assertThat(checks.get("order").passed()).isTrue();
	}

}
