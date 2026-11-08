package com.vettyou.applications;

/**
 * One row from a saved history file. Missing titles, employers, and salaries stay empty. Amounts are only kept when
 * the file names them.
 */
public record HistoryJobRequest(
		String company,
		String jobTitle,
		String location,
		String workArrangement,
		Object salaryMin,
		Object salaryMax,
		String salaryCurrency,
		String salaryUnit,
		String employmentType,
		String jobDescription,
		String technicalRequirements,
		String preferredQualifications,
		String source,
		String descriptionCompleteness) {
}
