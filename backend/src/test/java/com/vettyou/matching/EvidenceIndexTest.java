package com.vettyou.matching;

import static com.vettyou.matching.MatchFixtures.LIMINAL;
import static com.vettyou.matching.MatchFixtures.LIMINNALITY;
import static com.vettyou.matching.MatchFixtures.NANOHUB;
import static com.vettyou.matching.MatchFixtures.confirmed;
import static com.vettyou.matching.MatchFixtures.feature;
import static com.vettyou.matching.MatchFixtures.jake;
import static com.vettyou.matching.MatchFixtures.skill;
import static com.vettyou.matching.MatchFixtures.withClaims;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vettyou.evidence.ClaimView;

class EvidenceIndexTest {

	private final Library jake = jake();

	private EvidenceLine line(EvidenceIndex index, String start) {
		return index.lines().stream().filter(line -> line.text().startsWith(start)).findFirst().orElseThrow();
	}

	@Test
	void numbersJobSchoolAndProjectLinesWithWhereEachCameFrom() {
		EvidenceIndex index = EvidenceIndex.of(jake);

		EvidenceLine title = index.lines().getFirst();
		assertThat(title.ref()).isEqualTo("E1");
		assertThat(title.text()).isEqualTo("Freelance Full-Stack Developer");
		assertThat(title.label()).isEqualTo("Freelance Full-Stack Developer at Liminal Innovations, 2025-05 to present");
		assertThat(line(index, "Owned systems").group()).isEqualTo("job:" + LIMINAL);
		assertThat(line(index, "Owned systems").confirmed()).isTrue();
		assertThat(line(index, "B.S. Computer Science").label()).isEqualTo("Cal State Fullerton");
		assertThat(line(index, "Engineered a 12-stage").label()).isEqualTo("Liminnality");
		assertThat(line(index, "Engineered a 12-stage").confirmed()).isFalse();
		assertThat(index.lines()).noneMatch(line -> line.text().startsWith("- "));
	}

	@Test
	void aJobLineThatNamesAProjectIsTheSamePieceOfWork() {
		EvidenceIndex index = EvidenceIndex.of(jake);

		assertThat(line(index, "Identified the scheduling").group()).isEqualTo("project:" + LIMINNALITY);
		assertThat(line(index, "Built the Liminnality acquisition funnel").group()).isEqualTo("project:" + LIMINNALITY);
	}

	@Test
	void aLineHoldingABulletIsConfirmedOnceTheBulletIsKeptAndGoneOnceItIsNotMine() {
		String kept = "Frontend tests run in Vitest";
		String waiting = "Scans barcodes with React";
		String notMine = "Deployed the catalog with Kubernetes";
		Library library = withClaims(jake, List.of(feature(NANOHUB, kept, "approved"),
				skill(NANOHUB, "Vitest", "- " + kept, "candidate"), feature(NANOHUB, waiting, "candidate"),
				skill(NANOHUB, "React", waiting, "approved"), feature(NANOHUB, notMine, "rejected"),
				skill(NANOHUB, "Kubernetes", notMine, "approved")));

		EvidenceIndex index = EvidenceIndex.of(library);

		assertThat(line(index, kept).confirmed()).isTrue();
		assertThat(index.lines()).filteredOn(line -> line.text().equals(kept)).hasSize(1);
		assertThat(line(index, waiting).confirmed()).isFalse();
		assertThat(index.lines()).extracting(EvidenceLine::text).doesNotContain(notMine);
	}

	@Test
	void aLineOnlySkillsQuoteIsConfirmedOnlyWhenEachSkillOnItIsKept() {
		String shared = "Built with React and PostgreSQL";
		Library partly = withClaims(jake, List.of(skill(NANOHUB, "React", shared, "approved"),
				skill(NANOHUB, "PostgreSQL", shared, "candidate"), feature(NANOHUB, "Barcode scanning", "rejected"),
				new ClaimView(UUID.randomUUID(), NANOHUB, "feature", "Offline mode next", "feature", "Offline mode next",
						true, "approved")));

		EvidenceIndex index = EvidenceIndex.of(partly);

		assertThat(line(index, shared).confirmed()).isFalse();
		assertThat(index.lines()).extracting(EvidenceLine::text).doesNotContain("Barcode scanning", "Offline mode next");
		assertThat(line(EvidenceIndex.of(confirmed(partly)), shared).confirmed()).isTrue();
	}

	@Test
	void theFingerprintChangesWithWordingButNotWithConfirmation() {
		String before = EvidenceIndex.of(jake).fingerprint();
		Library edited = new Library(jake.claims(), jake.projectNames(),
				List.of(MatchFixtures.job(LIMINAL, "Liminal Innovations", "Freelance Full-Stack Developer",
						LocalDate.of(2025, 5, 1), null, true, "- Designed REST APIs for three clients")),
				jake.schools(), jake.profile());

		assertThat(EvidenceIndex.of(jake).fingerprint()).isEqualTo(before);
		assertThat(EvidenceIndex.of(confirmed(jake)).fingerprint()).isEqualTo(before);
		assertThat(EvidenceIndex.of(edited).fingerprint()).isNotEqualTo(before);
	}

	@Test
	void aStoredQuoteIsFoundAgainOnlyWhileTheLineStillHoldsIt() {
		EvidenceIndex index = EvidenceIndex.of(jake);

		assertThat(index.holding(new EvidenceRef("job", LIMINAL, "old wording", "Designed REST APIs"))).isPresent();
		assertThat(index.holding(new EvidenceRef("job", LIMINAL, "old wording", "Built a Kubernetes operator")))
			.isEmpty();
		assertThat(index.holding(new EvidenceRef("project", LIMINAL, "old wording", "Designed REST APIs"))).isEmpty();
	}

}
