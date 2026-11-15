package com.vettyou.resumes;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.vettyou.common.NotFoundException;
import com.vettyou.jobs.JobService;
import com.vettyou.jobs.Tailoring;
import com.vettyou.matching.MatchResult;
import com.vettyou.matching.MatchService;
import com.vettyou.matching.PartMatch;
import com.vettyou.matching.RankedLine;
import com.vettyou.matching.RequirementMatch;
import com.vettyou.matching.ResumePlan;
import com.vettyou.matching.ResumeSkill;
import com.vettyou.templates.AtsFirstTemplate;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Builds and keeps tailored resumes. Java picks every line from the person's confirmed data; nothing from the posting
 * is printed, and no AI is called. A built version is never changed: building again saves a new version.
 */
@Service
public class TailoredResumeService {

	private static final int FORMAT = 1;

	private final TailoredResumeRepository resumes;

	private final JobService jobs;

	private final MatchService matching;

	private final ResumeFitter fitter;

	private final AtsFirstTemplate template;

	private final JsonMapper json;

	public TailoredResumeService(TailoredResumeRepository resumes, JobService jobs, MatchService matching,
			ResumeFitter fitter, AtsFirstTemplate template, JsonMapper json) {
		this.resumes = resumes;
		this.jobs = jobs;
		this.matching = matching;
		this.fitter = fitter;
		this.template = template;
		this.json = json;
	}

	/** A downloadable file. */
	public record ResumeFile(String name, byte[] content) {
	}

	@Transactional
	public TailoredResumeResponse build(UUID userId, UUID postingId) {
		Tailoring posting = jobs.tailoring(userId, postingId);
		if (!posting.ready()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This posting has not been read yet.");
		}
		ResumePlan plan = matching.plan(userId, posting.requirements(), posting.fingerprint(), posting.body());
		ResumeFitter.Fit fit = fitter.fit(plan);
		AtsFirstTemplate.Rendered rendered = template.render(fit.document());
		String text = template.readBack(rendered.pdf());
		List<ReadCheck> checks = ReadBack.checks(fit.document(), plan.profile(), rendered, text);
		double readability = (double) checks.stream().filter(ReadCheck::passed).count() / checks.size();
		MatchResult match = matching.scoreResume(userId, posting.requirements(), fit.keys(), fit.skillRows(),
				readability);
		Snapshot snapshot = new Snapshot(FORMAT, posting.title(), posting.company(), fit.document(), match,
				plan.match().score().total(), plan.match().score().totalIfConfirmed(), readability, checks,
				fit.placed().stream().map(line -> ResumeLine.of(line, "")).toList(),
				fit.leftOff().stream().map(omitted -> ResumeLine.of(omitted.line(), omitted.reason())).toList(),
				plan.toConfirm().stream().map(line -> ResumeLine.of(line, "confirm")).toList(), fit.skills(),
				fit.skillsLeftOff(), suggestions(fit, plan, checks), workOn(match), rendered.pages());
		int version = resumes.lastVersion(postingId) + 1;
		try {
			TailoredResume saved = resumes.saveAndFlush(TailoredResume.create(userId, postingId, version,
					AtsFirstTemplate.ID, json.writeValueAsString(snapshot), text, rendered.pdf(), sha256(rendered.pdf()),
					plan.sources(), match.score().total(), posting.matchedAt()));
			return response(saved, snapshot, List.of());
		}
		catch (DataIntegrityViolationException ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Another version of this resume was saved at the same moment. Try again.");
		}
	}

	@Transactional(readOnly = true)
	public List<TailoredResumeSummary> list(UUID userId, UUID postingId) {
		jobs.requireOwned(userId, postingId);
		return resumes.summaries(postingId, userId);
	}

	/** Builds version 1 when this posting has none. Does not add versions on rematch. */
	public void ensureBuilt(UUID userId, UUID postingId) {
		if (!list(userId, postingId).isEmpty()) {
			return;
		}
		build(userId, postingId);
	}

	@Transactional(readOnly = true)
	public TailoredResumeResponse get(UUID userId, UUID postingId, UUID id) {
		TailoredResume resume = owned(userId, postingId, id);
		return response(resume, snapshot(resume), staleReasons(userId, resume));
	}

	@Transactional(readOnly = true)
	public ResumeFile pdf(UUID userId, UUID postingId, UUID id) {
		TailoredResume resume = owned(userId, postingId, id);
		return new ResumeFile(fileName(snapshot(resume).document().name()), resume.getPdf());
	}

	@Transactional(readOnly = true)
	public byte[] preview(UUID userId, UUID postingId, UUID id) {
		return template.preview(owned(userId, postingId, id).getPdf());
	}

	private TailoredResume owned(UUID userId, UUID postingId, UUID id) {
		return resumes.findByIdAndPostingIdAndUserId(id, postingId, userId).orElseThrow(NotFoundException::new);
	}

	private Snapshot snapshot(TailoredResume resume) {
		return json.readerFor(Snapshot.class)
			.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.readValue(resume.getSnapshot());
	}

	/** Why this version no longer matches what a new build would use. Empty when it is current. */
	private List<String> staleReasons(UUID userId, TailoredResume resume) {
		Tailoring posting = jobs.tailoring(userId, resume.getPostingId());
		if (posting.failed()) {
			return List.of("Reading the posting again failed, so this version can't be compared with it. Read it "
					+ "again from its match page.");
		}
		if (!posting.ready()) {
			return List.of("The posting is being read again. Build a new version once it is ready.");
		}
		List<String> reasons = new ArrayList<>();
		if (!Objects.equals(posting.matchedAt(), resume.getMatchedAt())) {
			reasons.add("The posting was checked against your evidence again after this version was built.");
		}
		ResumePlan plan = matching.plan(userId, posting.requirements(), posting.fingerprint(), posting.body());
		if (!plan.sources().equals(resume.getSourcesHash())) {
			reasons.add("Your profile, jobs, projects, education, skills, or answers changed after this version was "
					+ "built.");
		}
		return List.copyOf(reasons);
	}

	private TailoredResumeResponse response(TailoredResume resume, Snapshot snapshot, List<String> stale) {
		return new TailoredResumeResponse(resume.getId(), resume.getPostingId(), resume.getVersion(),
				resume.getTemplate(), resume.getCreatedAt(), snapshot.postingTitle(), snapshot.postingCompany(),
				!stale.isEmpty(), stale, snapshot.match().score(), snapshot.evidenceTotal(),
				snapshot.evidenceTotalIfConfirmed(), snapshot.match().requirements(), snapshot.readability(),
				snapshot.checks(), snapshot.lines(), snapshot.leftOff(), snapshot.toConfirm(), snapshot.skills(),
				snapshot.skillsLeftOff(), snapshot.suggestions(), snapshot.workOn(), snapshot.document(),
				resume.getPlainText(), snapshot.pages(), resume.getPdfSha256(), highlights(snapshot));
	}

	/** Boxes on the first-page PNG for printed lines that count toward the estimate. */
	private List<ResumeHighlight> highlights(Snapshot snapshot) {
		List<ResumeLine> scored = snapshot.lines().stream().filter((line) -> !line.backs().isEmpty()).toList();
		if (scored.isEmpty()) {
			return List.of();
		}
		Map<String, String> why = new LinkedHashMap<>();
		for (ResumeLine line : scored) {
			why.putIfAbsent(line.text(), why(line));
		}
		List<ResumeHighlight> marks = new ArrayList<>();
		for (AtsFirstTemplate.Mark mark : template.marks(snapshot.document(), List.copyOf(why.keySet()))) {
			marks.add(new ResumeHighlight(mark.text(), why.getOrDefault(mark.text(), ""),
					mark.boxes().stream()
						.map((box) -> new HighlightBox(box.x(), box.y(), box.width(), box.height()))
						.toList()));
		}
		return List.copyOf(marks);
	}

	private static String why(ResumeLine line) {
		LinkedHashMap<String, Boolean> parts = new LinkedHashMap<>();
		for (var back : line.backs()) {
			String text = back.part() + " — " + back.requirement();
			if ("transferable".equals(back.support())) {
				text += " (transferable)";
			}
			parts.putIfAbsent(text, true);
		}
		return String.join("; ", parts.keySet());
	}

	/** Changes to the person's own data that would let the next version show more. */
	private static List<ResumeSuggestion> suggestions(ResumeFitter.Fit fit, ResumePlan plan, List<ReadCheck> checks) {
		List<ResumeSuggestion> suggestions = new ArrayList<>();
		for (ResumeFitter.Omitted omitted : fit.leftOff()) {
			RankedLine line = omitted.line();
			if (ResumeFitter.SPACE.equals(omitted.reason()) && line.gain() > 0) {
				suggestions.add(new ResumeSuggestion("space", "This line matches the posting but did not fit on one "
						+ "page. Shortening it, or longer lines placed above it, would make room.", line.text(),
						line.kind(), line.sourceId()));
			}
			else if (ResumeFitter.TOO_LONG.equals(omitted.reason()) && !line.backs().isEmpty()) {
				suggestions.add(new ResumeSuggestion("too_long", "This line matches the posting but is too long to "
						+ "print as one bullet. Split it into shorter lines on its " + kindName(line.kind()) + " page.",
						line.text(), line.kind(), line.sourceId()));
			}
		}
		for (RankedLine line : plan.toConfirm()) {
			suggestions.add(new ResumeSuggestion("confirm", "This project line matches the posting. Confirm it on the "
					+ "project page if it is accurate, then build again.", line.text(), line.kind(), line.sourceId()));
		}
		if (!fit.skillsLeftOff().isEmpty()) {
			suggestions.add(new ResumeSuggestion("skills", "These kept skills did not fit: "
					+ String.join(", ", fit.skillsLeftOff().stream().map(ResumeSkill::shown).toList()) + ".", "", "",
					null));
		}
		for (ReadCheck check : checks) {
			if (!check.passed()) {
				suggestions.add(new ResumeSuggestion("readability", check.label() + ": " + check.detail(), "", "",
						null));
			}
		}
		return List.copyOf(suggestions);
	}

	/** What the posting asks for that no line of the person's can show yet, once each. */
	private static List<WorkOnItem> workOn(MatchResult match) {
		Map<String, WorkOnItem> items = new LinkedHashMap<>();
		for (RequirementMatch item : match.requirements()) {
			switch (item.status()) {
				case "gap" -> items.putIfAbsent(key(item.skill()), new WorkOnItem(item.skill(), "", item.group(), "gap",
						"You said you don't have this."));
				case "unclear" -> {
					if (item.canMarkMet() && item.answer() == null) {
						items.putIfAbsent(key(item.skill()), new WorkOnItem(item.skill(), "", item.group(), "unclear",
								"Your saved text can't settle this. Answer it on the match page."));
					}
				}
				case "direct", "met" -> {
				}
				default -> {
					if ("any".equals(item.logic())) {
						if (item.parts().stream().allMatch(part -> missing(part.status()))) {
							items.putIfAbsent(key(item.skill()), workOn(item, "", "not_shown"));
						}
						break;
					}
					for (PartMatch part : item.parts()) {
						if (missing(part.status())) {
							items.putIfAbsent(key(part.text()), workOn(item, part.text(), part.status()));
						}
					}
				}
			}
		}
		return List.copyOf(items.values());
	}

	private static boolean missing(String status) {
		return "not_shown".equals(status) || "changed".equals(status);
	}

	private static WorkOnItem workOn(RequirementMatch item, String part, String status) {
		String reason = "changed".equals(status)
				? "A line this relied on was edited or removed. Check the posting again from its match page."
				: "None of your saved lines show this. If you have done it, add it to a job or project.";
		return new WorkOnItem(item.skill(), part, item.group(), status, reason);
	}

	private static String key(String text) {
		return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
	}

	private static String kindName(String kind) {
		return "project".equals(kind) ? "project" : "job";
	}

	/** "Jake-Estrada-resume.pdf", or "resume.pdf" without a usable name. */
	static String fileName(String name) {
		String base = name.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-+|-+$", "");
		return base.isEmpty() ? "resume.pdf" : base + "-resume.pdf";
	}

	private static String sha256(byte[] content) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
