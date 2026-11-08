package com.vettyou.matching;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vettyou.profile.EducationResponse;
import com.vettyou.profile.ExperienceText;
import com.vettyou.profile.ProfileResponse;

/**
 * What a tailored resume can draw on for one posting, read in one go so every part agrees.
 * <ul>
 * <li>{@code match} is the posting's match against all saved evidence.</li>
 * <li>{@code lines} are the person's confirmed job and project lines, best first: lines that cover more of the posting,
 * then the rest by wording. A line too long to print or that reads as a note never counts as covering. Job titles and
 * schools are not listed; every resume prints them.</li>
 * <li>{@code projects} are the projects whose confirmed lines show part of the posting, the ones that show the most
 * first. A resume shows up to {@link #PROJECTS} of them, {@link #PROJECT_LINES} lines each, even when jobs show the
 * same.</li>
 * <li>{@code toConfirm} are project lines not kept yet that would cover more of the posting, or would put one of the
 * projects that could show the most of it on the resume.</li>
 * <li>{@code skills} are kept skills, the ones the posting names first.</li>
 * <li>{@code sources} changes whenever anything a resume would print or count changes.</li>
 * </ul>
 */
public record ResumePlan(MatchResult match, List<RankedLine> lines, List<RankedProject> projects,
		List<RankedLine> toConfirm, List<ResumeSkill> skills, String sources, ProfileResponse profile,
		List<ExperienceText> jobs, List<EducationResponse> schools, Map<UUID, String> projectNames) {

	public static final int PROJECTS = 3;

	public static final int PROJECT_LINES = 2;

}
