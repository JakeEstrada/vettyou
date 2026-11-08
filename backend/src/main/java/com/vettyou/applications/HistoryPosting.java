package com.vettyou.applications;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Turns a history row into posting text and application fields. Copies only what the file stated. Unknown employer
 * and unknown pay stay blank. Reference-only rows and missing titles are skipped.
 */
final class HistoryPosting {

	private HistoryPosting() {
	}

	static boolean skip(HistoryJobRequest job) {
		if (job == null) {
			return true;
		}
		String title = title(job);
		if (title.isEmpty() || title.equalsIgnoreCase("Role title unavailable")) {
			return true;
		}
		return text(job.descriptionCompleteness()).equalsIgnoreCase("reference only");
	}

	static String title(HistoryJobRequest job) {
		return clip(text(job.jobTitle()), 200);
	}

	static String company(HistoryJobRequest job) {
		String company = text(job.company());
		if (company.isEmpty() || company.equalsIgnoreCase("Unknown employer")) {
			return "";
		}
		return clip(company, 160);
	}

	static String location(HistoryJobRequest job) {
		return clip(text(job.location()), 120);
	}

	static String pay(HistoryJobRequest job) {
		String low = money(job.salaryMin(), job.salaryUnit());
		String high = money(job.salaryMax(), job.salaryUnit());
		if (low.isEmpty() && high.isEmpty()) {
			return "";
		}
		String range = low.isEmpty() ? high : high.isEmpty() || low.equals(high) ? low : low + " – " + high;
		if ("hour".equalsIgnoreCase(text(job.salaryUnit()))) {
			return clip(range + " per hour", 80);
		}
		return clip(range, 80);
	}

	static String notes(HistoryJobRequest job) {
		StringBuilder notes = new StringBuilder("Saved from history.");
		String completeness = text(job.descriptionCompleteness());
		if (!completeness.isEmpty()) {
			notes.append(" Completeness: ").append(completeness).append('.');
		}
		String source = text(job.source());
		if (!source.isEmpty()) {
			notes.append(" Source: ").append(source).append('.');
		}
		if (company(job).isEmpty()) {
			notes.append(" Employer was not named in the saved history.");
		}
		return clip(notes.toString(), 4000);
	}

	static String body(HistoryJobRequest job) {
		StringBuilder body = new StringBuilder();
		body.append("# ").append(title(job)).append('\n');
		String description = text(job.jobDescription());
		if (!description.isEmpty()) {
			body.append('\n').append(description).append('\n');
		}
		section(body, "Technical requirements", job.technicalRequirements());
		section(body, "Preferred qualifications", job.preferredQualifications());
		body.append('\n');
		labeled(body, "Company", company(job));
		labeled(body, "Location", location(job));
		labeled(body, "Work arrangement", text(job.workArrangement()));
		String pay = pay(job);
		if (!pay.isEmpty()) {
			labeled(body, pay.toLowerCase(Locale.ROOT).contains("per hour") ? "Hourly rate" : "Salary", pay);
		}
		labeled(body, "Type", text(job.employmentType()));
		labeled(body, "Source", text(job.source()));
		labeled(body, "Completeness", text(job.descriptionCompleteness()));
		return clip(body.toString().strip(), 60_000);
	}

	static String key(String title, String company) {
		return text(title).toLowerCase(Locale.ROOT) + '\n' + text(company).toLowerCase(Locale.ROOT);
	}

	private static void section(StringBuilder body, String heading, String value) {
		String text = text(value);
		if (text.isEmpty()) {
			return;
		}
		body.append('\n').append("## ").append(heading).append('\n');
		if (text.contains(";")) {
			for (String part : text.split(";")) {
				String item = part.strip();
				if (!item.isEmpty()) {
					body.append("- ").append(item).append('\n');
				}
			}
		}
		else {
			body.append(text).append('\n');
		}
	}

	private static void labeled(StringBuilder body, String label, String value) {
		if (!value.isEmpty()) {
			body.append(label).append(": ").append(value).append('\n');
		}
	}

	private static String money(Object value, String unit) {
		BigDecimal amount = decimal(value);
		if (amount == null) {
			return "";
		}
		NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
		if ("hour".equalsIgnoreCase(text(unit)) && amount.stripTrailingZeros().scale() > 0) {
			format.setMinimumFractionDigits(2);
			format.setMaximumFractionDigits(2);
		}
		else {
			format.setMaximumFractionDigits(0);
		}
		return "$" + format.format(amount);
	}

	private static BigDecimal decimal(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof BigDecimal amount) {
			return amount;
		}
		if (value instanceof Number number) {
			return new BigDecimal(number.toString());
		}
		String text = value.toString().strip();
		if (text.isEmpty()) {
			return null;
		}
		try {
			return new BigDecimal(text);
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	private static String text(String value) {
		return value == null ? "" : value.replaceAll("\\s+", " ").strip();
	}

	private static String clip(String text, int limit) {
		if (text.length() <= limit) {
			return text;
		}
		return text.substring(0, limit).strip();
	}

}
