package com.vettyou.matching;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vettyou.ai.OpenAiClient;
import com.vettyou.evidence.ClaimView;
import com.vettyou.evidence.StackDetector;
import com.vettyou.profile.EducationResponse;
import com.vettyou.profile.ExperienceText;
import com.vettyou.profile.ProfileResponse;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * People, postings, and model answers for matching tests. Model answers are written by hand; everything after them
 * (citation checks, cross-checks, scoring) is the real code.
 */
final class MatchFixtures {

	static final UUID USER = UUID.randomUUID();

	static final UUID LIMINAL = UUID.randomUUID();

	static final UUID WOODWORKS = UUID.randomUUID();

	static final UUID UPS = UUID.randomUUID();

	static final UUID LIMINNALITY = UUID.randomUUID();

	static final UUID NANOHUB = UUID.randomUUID();

	static final UUID CSUF = UUID.randomUUID();

	static final UUID SADDLEBACK = UUID.randomUUID();

	static final JsonMapper JSON = JsonMapper.builder().build();

	static final StackDetector DETECTOR = new StackDetector();

	private MatchFixtures() {
	}

	/** Mirrors the imported resume: job write-ups count as written, project lines are still candidates. */
	static Library jake() {
		return new Library(
				List.of(feature(LIMINNALITY, "Engineered a 12-stage sales and production pipeline with automated transitions", "candidate"),
						feature(LIMINNALITY, "One resume says time tracking cut a two-hour manual process to about two minutes", "candidate"),
						feature(LIMINNALITY, "Frontend tests run in Vitest. API tests run in Node's test runner", "candidate"),
						feature(NANOHUB, "I owned the admin app and the REST endpoints for product records", "candidate"),
						skill(NANOHUB, "Angular", "Admin app built with Angular and a REST API", "candidate")),
				Map.of(LIMINNALITY, "Liminnality", NANOHUB, "NanoHub"),
				List.of(job(LIMINAL, "Liminal Innovations", "Freelance Full-Stack Developer", LocalDate.of(2025, 5, 1), null, true, """
						- Owned systems from requirements discovery through architecture, deployment, troubleshooting, and production support
						- Designed, architected, and deployed production systems using TypeScript, React, Node.js, Express, and MongoDB
						- Designed REST APIs, Socket.IO real-time sync, JWT authentication, and role-based access control
						- Designed role-based client and admin workflows for inventory, scheduling, and invoicing
						- Cover the UI with Vitest and the API with Node's test runner
						- Diagnose production issues, write down the cause and the fix, and add a test so it stays fixed
						- Work directly with businesses to understand their operations and develop custom software that automates administrative processes
						- Use SQL against PostgreSQL and MongoDB to investigate data issues, generate reports, and verify fixes
						- Built the Liminnality acquisition funnel with admin metrics and site engagement analytics"""),
						job(WOODWORKS, "Estrada Woodworks", "Operations Manager", LocalDate.of(2016, 1, 1), LocalDate.of(2025, 4, 1), false, """
						- Managed projects from customer inquiry through estimating, contract, scheduling, production, and install
						- Built spreadsheets, quoting tools, and other automation for estimating, scheduling, accounting, and job tracking
						- Wrote a Python script that reconciles QuickBooks balance-sheet and bank CSVs
						- Identified the scheduling and tracking bottlenecks, defined the requirements, and built Liminnality to replace them
						- Translated operator and office needs into product requirements
						- Point of contact for customers, vendors, office staff, shop teams, and installers"""),
						job(UPS, "UPS", "Operations Supervisor", LocalDate.of(2014, 6, 1), LocalDate.of(2016, 1, 1), false, """
						- Supervised and trained warehouse teams of up to 20
						- Kept sort operations on schedule during peak volume""")),
				List.of(new EducationResponse(CSUF, "Cal State Fullerton", "B.S.", "Computer Science", null,
						LocalDate.of(2026, 5, 1)),
						new EducationResponse(SADDLEBACK, "Saddleback College", "A.A.", "Mathematics", null, null)),
				new ProfileResponse("Jake Estrada", "", "", "Lake Forest, CA", "", ""));
	}

	/** A registered nurse: a strong fit in another field, so the same code must score software postings low. */
	static Library nurse() {
		UUID hospital = UUID.randomUUID();
		return new Library(List.of(), Map.of(),
				List.of(job(hospital, "Mission Hospital", "Registered Nurse", LocalDate.of(2018, 3, 1), null, true, """
						- Provide bedside care for 5 to 6 telemetry patients per shift
						- Chart assessments and medications in Epic EHR
						- Precept new graduate nurses during their first 12 weeks
						- Coordinate discharge plans with physicians, case managers, and families""")),
				List.of(new EducationResponse(UUID.randomUUID(), "Cal State Long Beach", "BSN", "Nursing", null, null)),
				new ProfileResponse("Sam Rivera", "sam.rivera@example.com", "(714) 555-0100", "Irvine, CA", "", ""));
	}

	/** The same person after confirming every project line. */
	static Library confirmed(Library library) {
		return withClaims(library, library.claims().stream().map(claim -> new ClaimView(claim.id(), claim.projectId(),
				claim.kind(), claim.name(), claim.category(), claim.quote(), claim.planned(), "approved")).toList());
	}

	static Library withClaims(Library library, List<ClaimView> claims) {
		return new Library(claims, library.projectNames(), library.jobs(), library.schools(), library.profile());
	}

	static Library withProfile(Library library, ProfileResponse profile) {
		return new Library(library.claims(), library.projectNames(), library.jobs(), library.schools(), profile);
	}

	static ExperienceText job(UUID id, String employer, String title, LocalDate start, LocalDate end, boolean current,
			String body) {
		return new ExperienceText(id, employer, title, "", start, end, current, body);
	}

	static ClaimView feature(UUID project, String line, String status) {
		return new ClaimView(UUID.randomUUID(), project, "feature", line, "feature", line, false, status);
	}

	static ClaimView skill(UUID project, String name, String line, String status) {
		return new ClaimView(UUID.randomUUID(), project, "skill", name, "framework", line, false, status);
	}

	/** A posting item whose parts have not been judged yet. */
	static RequirementInput item(String kind, String importance, String logic, String category, String quote,
			String... partsAndConcepts) {
		List<PartInput> parts = new ArrayList<>();
		for (int index = 0; index < partsAndConcepts.length; index += 2) {
			parts.add(new PartInput(UUID.randomUUID(), partsAndConcepts[index], partsAndConcepts[index + 1], null, "",
					List.of()));
		}
		return new RequirementInput(UUID.randomUUID(), quote.length() > 60 ? quote.substring(0, 60) : quote, kind,
				importance, category, quote, logic, null, null, List.copyOf(parts));
	}

	/** A part as stored after judging, citing the first line of {@code library} that holds each excerpt. */
	static PartInput judged(Library library, String text, String concept, String support, String... excerpts) {
		EvidenceIndex index = EvidenceIndex.of(library);
		List<EvidenceRef> evidence = new ArrayList<>();
		for (String excerpt : excerpts) {
			EvidenceLine line = index.lines()
				.stream()
				.filter(candidate -> Excerpts.in(excerpt, candidate.text()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No line holds: " + excerpt));
			evidence.add(new EvidenceRef(line.kind(), line.sourceId(), line.text(), excerpt));
		}
		return new PartInput(UUID.randomUUID(), text, concept, support, "Stored reason.", List.copyOf(evidence));
	}

	static RequirementInput stored(String kind, String importance, String logic, String quote, PartInput... parts) {
		return new RequirementInput(UUID.randomUUID(), quote, kind, importance, "experience", quote, logic, null, null,
				List.of(parts));
	}

	/** Scores stored judgments the way the posting page does, without calling the model. */
	static MatchResult score(Library library, List<RequirementInput> requirements) {
		return service(library, "{}").match(USER, requirements, "");
	}

	static RequirementInput answered(RequirementInput requirement, String answer) {
		return new RequirementInput(requirement.id(), requirement.skill(), requirement.kind(), requirement.importance(),
				requirement.category(), requirement.quote(), requirement.logic(), answer, java.time.Instant.now(),
				requirement.parts());
	}

	/** One citation as the model would send it. A null line means "the first line holding the excerpt". */
	record Quote(String line, String excerpt) {
	}

	/** One judgment as the model would send it. */
	record Said(String part, String support, String reason, List<Quote> quotes) {
	}

	static Said said(String part, String support, String... excerpts) {
		return new Said(part, support, "Model reason for " + part + ".",
				java.util.Arrays.stream(excerpts).map(excerpt -> new Quote(null, excerpt)).toList());
	}

	/** A citation pinned to the line holding {@code anchor}, quoting {@code excerpt}, which may not be in it. */
	static Said saidOn(String part, String support, String anchor, String excerpt) {
		return new Said(part, support, "Model reason for " + part + ".", List.of(new Quote(anchor, excerpt)));
	}

	static String answer(EvidenceIndex index, Said... judgments) {
		ObjectNode root = JSON.createObjectNode();
		ArrayNode list = root.putArray("judgments");
		for (Said said : judgments) {
			ObjectNode node = list.addObject();
			node.put("part", said.part());
			node.put("support", said.support());
			node.put("reason", said.reason());
			ArrayNode evidence = node.putArray("evidence");
			for (Quote quote : said.quotes()) {
				String anchor = quote.line() == null ? quote.excerpt() : quote.line();
				EvidenceLine line = index.lines()
					.stream()
					.filter(candidate -> Excerpts.in(anchor, candidate.text()))
					.findFirst()
					.orElseThrow(() -> new AssertionError("No line holds: " + anchor));
				evidence.addObject().put("line", line.ref()).put("excerpt", quote.excerpt());
			}
		}
		return JSON.writeValueAsString(root);
	}

	/** A matcher whose model answers {@code answer} and whose loader returns {@code library}. */
	static MatchService service(Library library, String answer) {
		LibraryLoader loader = mock(LibraryLoader.class);
		when(loader.load(USER)).thenReturn(library);
		OpenAiClient ai = mock(OpenAiClient.class);
		when(ai.enabled()).thenReturn(true);
		when(ai.model()).thenReturn("test-model");
		when(ai.structured(anyString(), anyString(), eq("evidence_match"), any())).thenReturn(JSON.readTree(answer));
		return new MatchService(loader, DETECTOR, new EvidenceMatcher(ai, JSON));
	}

	/** What the posting page sees after a model run: judgments stored on the parts, then scored. */
	static MatchResult run(Library library, List<RequirementInput> posting, Said... judgments) {
		MatchService service = service(library, answer(EvidenceIndex.of(library), judgments));
		Judgments judged = service.judge(USER, "", posting);
		return service.match(USER, stored(posting, judged), judged.fingerprint());
	}

	static List<RequirementInput> stored(List<RequirementInput> posting, Judgments judged) {
		return posting.stream().map(requirement -> new RequirementInput(requirement.id(), requirement.skill(),
				requirement.kind(), requirement.importance(), requirement.category(), requirement.quote(),
				requirement.logic(), requirement.answer(), requirement.answeredAt(), requirement.parts().stream().map(part -> {
					PartJudgment judgment = judged.parts().get(part.id());
					return judgment == null ? part : new PartInput(part.id(), part.text(), part.concept(),
							judgment.support(), judgment.reason(), judgment.evidence());
				}).toList())).toList();
	}

	static RequirementMatch find(MatchResult result, String quoteStart) {
		return result.requirements()
			.stream()
			.filter(match -> match.quote().startsWith(quoteStart))
			.findFirst()
			.orElseThrow(() -> new AssertionError("No requirement starts with: " + quoteStart));
	}

	static PartMatch part(RequirementMatch match, String text) {
		return match.parts()
			.stream()
			.filter(part -> part.text().equals(text))
			.findFirst()
			.orElseThrow(() -> new AssertionError("No part: " + text));
	}

	static Map<String, Double> factors(MatchScore score, boolean ifConfirmed) {
		Map<String, Double> values = new LinkedHashMap<>();
		score.factors().forEach(factor -> values.put(factor.key(), ifConfirmed ? factor.valueIfConfirmed() : factor.value()));
		return values;
	}

}
