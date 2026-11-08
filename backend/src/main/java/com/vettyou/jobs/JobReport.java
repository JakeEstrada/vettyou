package com.vettyou.jobs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import com.vettyou.matching.EvidenceCite;
import com.vettyou.matching.MatchLabels;
import com.vettyou.matching.MatchScore;
import com.vettyou.matching.PartMatch;
import com.vettyou.matching.Readiness;
import com.vettyou.matching.RequirementMatch;
import com.vettyou.matching.ScoreFactor;

/**
 * A shareable write-up of one job match: the score with its formula filled in, every requirement with its parts and
 * the evidence used, and the posting. The same lines feed the text and the PDF so they never disagree.
 */
@Component
class JobReport {

	enum Style {
		TITLE, HEADING, BODY, DETAIL
	}

	record Line(Style style, String text) {
	}

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
		.withZone(ZoneOffset.UTC);

	private static final List<String> GROUPS = List.of("required", "responsibility", "preferred");

	private static final float MARGIN = 54;

	private static final float SIZE = 10;

	private static final float LEADING = 13;

	List<Line> lines(JobPostingResponse posting) {
		List<Line> lines = new ArrayList<>();
		MatchScore score = posting.score();
		lines.add(new Line(Style.TITLE, "Vettyou job match report"));
		String title = posting.title().isBlank() ? "Job posting" : posting.title();
		lines.add(new Line(Style.BODY, posting.company().isBlank() ? title : title + " - " + posting.company()));
		lines.add(new Line(Style.BODY, "Read by " + posting.reader() + ". Evidence checked by "
				+ (posting.matcher().isBlank() ? "the keyword rules" : posting.matcher())
				+ (posting.matchedAt() == null ? "" : " on " + date(posting.matchedAt())) + ". Posted to Vettyou "
				+ date(posting.createdAt()) + ". Posting id " + posting.id() + "."));
		if (posting.evidenceChanged()) {
			lines.add(new Line(Style.BODY, "Your saved evidence changed after this check. Re-check to include it."));
		}
		lines.add(blank());

		lines.add(new Line(Style.HEADING, "Score: " + total(score.total()) + " / 100 (" + score.label()
				+ ", confirmed evidence only)"));
		if (score.totalIfConfirmed() != null && !score.totalIfConfirmed().equals(score.total())) {
			lines.add(new Line(Style.BODY, "If you confirm the " + score.toConfirm() + " flagged project line"
					+ (score.toConfirm() == 1 ? "" : "s") + ": " + score.totalIfConfirmed() + " / 100."));
		}
		if (score.toAnswer() > 0) {
			lines.add(new Line(Style.BODY, score.toAnswer() + " item" + (score.toAnswer() == 1 ? " needs" : "s need")
					+ " your answer before it counts."));
		}
		lines.add(new Line(Style.BODY, score.note()));
		lines.add(blank());
		lines.add(new Line(Style.BODY, "How the score is built (each factor 0 to 1; a factor left out drops from the "
				+ "divisor):"));
		for (ScoreFactor factor : score.factors()) {
			lines.add(factor(factor));
		}
		lines.add(new Line(Style.BODY, "Formula now: " + score.formula()));
		lines.add(new Line(Style.BODY, "Formula if confirmed: " + score.formulaIfConfirmed()));
		lines.add(blank());

		Readiness readiness = score.readiness();
		lines.add(new Line(Style.HEADING, "Application readiness (not part of the score)"));
		lines.add(new Line(Style.BODY, readiness.checks().stream().filter(Readiness.Check::done).count() + " of "
				+ readiness.checks().size() + ": " + String.join(", ", readiness.checks()
					.stream()
					.map(check -> check.label() + (check.done() ? " yes" : " missing"))
					.toList()) + "."));
		lines.add(blank());

		lines.add(new Line(Style.HEADING, "How items are credited"));
		lines.add(new Line(Style.BODY, "Direct evidence counts 1, transferable 0.5, not shown 0. Support that rests "
				+ "only on project lines you have not confirmed counts 0 now and its full value in the \"if confirmed\" "
				+ "score."));
		lines.add(new Line(Style.BODY, "Parts that need clarification (years, eligibility) are left out until you "
				+ "answer. \"I meet this\" counts 1; \"I don't have this\" counts 0 and is marked a confirmed gap."));
		lines.add(new Line(Style.BODY, "\"Any one\" items take their best part; other items average their parts. In a "
				+ "group, parts that repeat an idea from another item share one part's weight. A group's value is the "
				+ "sum of weight x credit over its items divided by the sum of their weights."));
		lines.add(new Line(Style.BODY, "The posting is never used as evidence about you."));

		for (String group : GROUPS) {
			group(lines, group, posting.requirements());
		}

		lines.add(blank());
		lines.add(new Line(Style.HEADING, "The posting as pasted"));
		for (String raw : posting.body().split("\\R", -1)) {
			lines.add(new Line(Style.DETAIL, raw));
		}
		return lines;
	}

	String text(JobPostingResponse posting) {
		StringBuilder out = new StringBuilder();
		for (Line line : lines(posting)) {
			switch (line.style()) {
				case TITLE -> out.append("# ").append(line.text()).append('\n');
				case HEADING -> out.append("## ").append(line.text()).append('\n');
				case DETAIL -> out.append("   ").append(line.text()).append('\n');
				default -> out.append(line.text()).append('\n');
			}
		}
		return out.toString();
	}

	byte[] pdf(JobPostingResponse posting) {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			new Writer(document).write(lines(posting));
			document.save(out);
			return out.toByteArray();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static void group(List<Line> lines, String group, List<RequirementMatch> all) {
		List<RequirementMatch> items = all.stream().filter(item -> group.equals(item.group())).toList();
		if (items.isEmpty()) {
			return;
		}
		lines.add(blank());
		lines.add(new Line(Style.HEADING, MatchLabels.group(group) + " (" + items.size() + ")"));
		int number = 1;
		double weights = 0;
		double points = 0;
		double pointsIfConfirmed = 0;
		for (RequirementMatch item : items) {
			lines.add(new Line(Style.BODY, number++ + ". " + item.skill() + "  [" + item.kind() + ", " + item.category()
					+ ", " + ("any".equals(item.logic()) ? "any one part is enough" : "needs every part") + "]"));
			lines.add(new Line(Style.DETAIL, "Status: " + MatchLabels.requirement(item.status()) + ". "
					+ (item.weight() == null ? "Left out until answered."
							: "Weight " + number(item.weight()) + ", credit " + number(item.credit()) + " (if confirmed "
									+ number(item.creditIfConfirmed()) + ")"
									+ (item.strength() == null ? "." : ", strength " + number(item.strength()) + "."))));
			lines.add(new Line(Style.DETAIL, "Why: " + item.note()));
			for (PartMatch part : item.parts()) {
				lines.add(new Line(Style.DETAIL, "Part: " + part.text() + " - " + MatchLabels.part(part.status())
						+ (part.weight() == null ? "" : " (weight " + number(part.weight()) + ")")
						+ (part.sharedBy() > 1 ? ", same idea in " + part.sharedBy() + " items" : "")
						+ (part.reason().isBlank() ? "" : ". " + part.reason())));
				for (EvidenceCite cite : part.evidence()) {
					lines.add(new Line(Style.DETAIL, "   Evidence (" + cite.kind() + ", " + cite.label()
							+ (cite.confirmed() ? "" : ", not confirmed yet") + "): \"" + cite.excerpt() + "\""));
				}
			}
			lines.add(new Line(Style.DETAIL, "Posting says: \"" + item.quote() + "\""));
			if (item.weight() != null) {
				weights += item.weight();
				points += item.weight() * item.credit();
				pointsIfConfirmed += item.weight() * item.creditIfConfirmed();
			}
		}
		if (weights > 0) {
			lines.add(new Line(Style.BODY, String.format(Locale.ROOT,
					"Group value: %.4f / %.4f = %.4f now; %.4f / %.4f = %.4f if confirmed.", points, weights,
					points / weights, pointsIfConfirmed, weights, pointsIfConfirmed / weights)));
		}
	}

	private static Line factor(ScoreFactor factor) {
		if (factor.value() == null && factor.valueIfConfirmed() == null) {
			return new Line(Style.DETAIL, factor.label() + ": n/a, left out (weight " + factor.weight() + "). "
					+ factor.how());
		}
		return new Line(Style.DETAIL, factor.label() + ": " + factorValue(factor.value()) + " now, "
				+ factorValue(factor.valueIfConfirmed()) + " if confirmed; weight " + weight(factor) + "; "
				+ (factor.value() == null || factor.activeWeight() == null ? "left out now"
						: factorValue(factor.activeWeight() * factor.value()) + " points now")
				+ ". " + factor.how());
	}

	private static String weight(ScoreFactor factor) {
		if (!"strength".equals(factor.key())) {
			return String.valueOf(factor.weight());
		}
		return factor.weight() + " x coverage = " + factorValue(factor.activeWeight()) + " now, "
				+ factorValue(factor.activeWeightIfConfirmed()) + " if confirmed";
	}

	private static String number(Double value) {
		return value == null ? "n/a" : String.format(Locale.ROOT, "%.3f", value);
	}

	/** The four decimals the formula uses. */
	private static String factorValue(Double value) {
		return value == null ? "n/a" : String.format(Locale.ROOT, "%.4f", value);
	}

	private static String total(Integer total) {
		return total == null ? "n/a" : String.valueOf(total);
	}

	private static String date(Instant instant) {
		return DATE.format(instant);
	}

	private static Line blank() {
		return new Line(Style.BODY, "");
	}

	/** Lays out lines on letter pages with word wrap. Only Helvetica's character set is drawable; others print as "?". */
	private static final class Writer {

		private final PDDocument document;

		private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

		private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

		private final Map<Integer, Boolean> drawable = new HashMap<>();

		private PDPageContentStream stream;

		private float y;

		private int pageNumber;

		Writer(PDDocument document) {
			this.document = document;
		}

		void write(List<Line> lines) throws IOException {
			newPage();
			for (Line line : lines) {
				PDType1Font font = line.style() == Style.BODY || line.style() == Style.DETAIL ? regular : bold;
				float size = line.style() == Style.TITLE ? 16 : line.style() == Style.HEADING ? 12 : SIZE;
				float indent = line.style() == Style.DETAIL ? 18 : 0;
				if (line.style() == Style.HEADING) {
					y -= 4;
				}
				String text = safe(line.text().replace("\t", "    "));
				for (String piece : wrap(text, font, size, PDRectangle.LETTER.getWidth() - 2 * MARGIN - indent)) {
					if (y < MARGIN + LEADING) {
						newPage();
					}
					stream.beginText();
					stream.setFont(font, size);
					stream.newLineAtOffset(MARGIN + indent, y);
					stream.showText(piece);
					stream.endText();
					y -= size > SIZE ? size + 5 : LEADING;
				}
			}
			stream.close();
		}

		private void newPage() throws IOException {
			if (stream != null) {
				stream.close();
			}
			PDPage page = new PDPage(PDRectangle.LETTER);
			document.addPage(page);
			stream = new PDPageContentStream(document, page);
			pageNumber++;
			stream.beginText();
			stream.setFont(regular, 8);
			stream.newLineAtOffset(MARGIN, MARGIN / 2);
			stream.showText("Vettyou job match report - page " + pageNumber);
			stream.endText();
			y = PDRectangle.LETTER.getHeight() - MARGIN;
		}

		private List<String> wrap(String text, PDType1Font font, float size, float width) throws IOException {
			List<String> pieces = new ArrayList<>();
			if (text.isEmpty()) {
				pieces.add("");
				return pieces;
			}
			StringBuilder current = new StringBuilder();
			for (String word : text.split(" ", -1)) {
				String candidate = current.isEmpty() ? word : current + " " + word;
				if (width(candidate, font, size) <= width) {
					current.setLength(0);
					current.append(candidate);
					continue;
				}
				if (!current.isEmpty()) {
					pieces.add(current.toString());
					current.setLength(0);
				}
				String rest = word;
				while (width(rest, font, size) > width) {
					int cut = rest.length() - 1;
					while (cut > 1 && width(rest.substring(0, cut), font, size) > width) {
						cut--;
					}
					pieces.add(rest.substring(0, cut));
					rest = rest.substring(cut);
				}
				current.append(rest);
			}
			pieces.add(current.toString());
			return pieces;
		}

		private static float width(String text, PDType1Font font, float size) throws IOException {
			return font.getStringWidth(text) / 1000 * size;
		}

		private String safe(String text) {
			StringBuilder out = new StringBuilder(text.length());
			text.codePoints().forEach(point -> out.appendCodePoint(drawable.computeIfAbsent(point, this::canDraw) ? point : '?'));
			return out.toString();
		}

		private boolean canDraw(int point) {
			try {
				regular.encode(new String(Character.toChars(point)));
				bold.encode(new String(Character.toChars(point)));
				return true;
			}
			catch (IOException | IllegalArgumentException ex) {
				return false;
			}
		}

	}

}
