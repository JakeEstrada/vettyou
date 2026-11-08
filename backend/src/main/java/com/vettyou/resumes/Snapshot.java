package com.vettyou.resumes;

import java.util.List;

import com.vettyou.matching.MatchResult;
import com.vettyou.matching.ResumeSkill;
import com.vettyou.templates.ResumeDocument;

/** Everything a version showed when it was built, stored as JSON so it reads the same later. */
record Snapshot(
		int format,
		String postingTitle,
		String postingCompany,
		ResumeDocument document,
		MatchResult match,
		Integer evidenceTotal,
		Integer evidenceTotalIfConfirmed,
		double readability,
		List<ReadCheck> checks,
		List<ResumeLine> lines,
		List<ResumeLine> leftOff,
		List<ResumeLine> toConfirm,
		List<ResumeSkill> skills,
		List<ResumeSkill> skillsLeftOff,
		List<ResumeSuggestion> suggestions,
		List<WorkOnItem> workOn,
		int pages) {
}
