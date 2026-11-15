package com.vettyou.resumes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.vettyou.matching.MatchScore;
import com.vettyou.matching.RequirementMatch;
import com.vettyou.matching.ResumeSkill;
import com.vettyou.templates.ResumeDocument;

/**
 * One saved version of a tailored resume, exactly as built.
 * <ul>
 * <li>{@code score} is the Vettyou estimate for what this resume shows; {@code evidenceTotal} is the estimate for all
 * saved evidence when it was built.</li>
 * <li>{@code stale} is true when the person's data or the posting's match changed since; {@code staleReasons} say
 * what.</li>
 * <li>{@code lines} are on the resume; {@code leftOff} are not, with a reason; {@code toConfirm} are project lines that
 * would help once confirmed.</li>
 * </ul>
 */
public record TailoredResumeResponse(
		UUID id,
		UUID postingId,
		int version,
		String template,
		Instant createdAt,
		String postingTitle,
		String postingCompany,
		boolean stale,
		List<String> staleReasons,
		MatchScore score,
		Integer evidenceTotal,
		Integer evidenceTotalIfConfirmed,
		List<RequirementMatch> requirements,
		double readability,
		List<ReadCheck> checks,
		List<ResumeLine> lines,
		List<ResumeLine> leftOff,
		List<ResumeLine> toConfirm,
		List<ResumeSkill> skills,
		List<ResumeSkill> skillsLeftOff,
		List<ResumeSuggestion> suggestions,
		List<WorkOnItem> workOn,
		ResumeDocument document,
		String plainText,
		int pages,
		String pdfSha256,
		List<ResumeHighlight> highlights) {
}
