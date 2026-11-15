package com.vettyou.templates;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * The ATS-first template: one column, a standard font, plain section headings, and no tables, images, or text boxes,
 * so an applicant tracking system reads the text back in the order a person sees it.
 */
@Component
public class AtsFirstTemplate {

	public static final String ID = "ats-first";

	private static final PDRectangle PAGE = PDRectangle.LETTER;

	private static final float MARGIN_X = 50;

	private static final float MARGIN_Y = 42;

	private static final float WIDTH = PAGE.getWidth() - 2 * MARGIN_X;

	private static final float NAME_SIZE = 18;

	private static final float CONTACT_SIZE = 9.5f;

	private static final float HEADING_SIZE = 10.5f;

	private static final float BODY_SIZE = 10;

	private static final float LEADING = 12.5f;

	private static final float BULLET_X = 8;

	private static final float TEXT_X = 18;

	private static final float DATES_GAP = 12;

	private static final float PREVIEW_DPI = 100;

	private static final String SEPARATOR = "  |  ";

	/** Spacing and dash variants the standard font lacks, written the plain way. They never change what a line says. */
	private static final Map<Integer, String> PLAIN = Map.of(0x00A0, " ", 0x2007, " ", 0x2009, " ", 0x202F, " ",
			0x2010, "-", 0x2011, "-", 0x2212, "-", 0x200B, "", 0xFEFF, "");

	private static final Map<Integer, Boolean> PRINTS = new ConcurrentHashMap<>();

	/**
	 * A drawn resume. {@code fragments} are the printed pieces of text in reading order, as printed. {@code unprintable}
	 * lists characters the font has no glyph for; each was printed as "?".
	 */
	public record Rendered(byte[] pdf, int pages, List<String> fragments, List<String> unprintable) {
	}

	public int pages(ResumeDocument document) {
		return layout(document, new Fonts()).pages;
	}

	public Rendered render(ResumeDocument document) {
		Fonts fonts = new Fonts();
		Layout layout = layout(document, fonts);
		try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			PDDocumentInformation info = pdf.getDocumentInformation();
			String name = document.name().strip();
			info.setTitle(name.isEmpty() ? "Resume" : name + " resume");
			if (!name.isEmpty()) {
				info.setAuthor(name);
			}
			for (int page = 0; page < layout.pages; page++) {
				PDPage target = new PDPage(PAGE);
				pdf.addPage(target);
				try (PDPageContentStream stream = new PDPageContentStream(pdf, target)) {
					stream.setStrokingColor(0.55f, 0.55f, 0.55f);
					stream.setLineWidth(0.6f);
					for (Rule rule : layout.rules) {
						if (rule.page() == page) {
							stream.moveTo(MARGIN_X, rule.y());
							stream.lineTo(MARGIN_X + WIDTH, rule.y());
							stream.stroke();
						}
					}
					for (Op op : layout.ops) {
						if (op.page() == page) {
							stream.beginText();
							stream.setFont(op.font(), op.size());
							stream.newLineAtOffset(op.x(), op.y());
							stream.showText(op.text());
							stream.endText();
						}
					}
				}
			}
			pdf.save(out);
			return new Rendered(out.toByteArray(), layout.pages, List.copyOf(layout.fragments),
					List.copyOf(layout.unprintable));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/** The text an applicant tracking system would pull out of the PDF, top to bottom. */
	public String readBack(byte[] pdf) {
		try (PDDocument document = Loader.loadPDF(pdf)) {
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setSortByPosition(true);
			return stripper.getText(document);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * Boxes on the first-page PNG for printed bullets. Coordinates are pixels at {@link #PREVIEW_DPI}, origin top-left,
	 * matching {@link #preview(byte[])}. Empty when a line is not on page 1.
	 */
	public List<Mark> marks(ResumeDocument document, List<String> lines) {
		Fonts fonts = new Fonts();
		Layout layout = layout(document, fonts);
		List<Mark> marks = new ArrayList<>();
		for (String line : lines) {
			List<Box> boxes = firstPageBoxes(layout, fonts, line);
			if (!boxes.isEmpty()) {
				marks.add(new Mark(line, boxes));
			}
		}
		return List.copyOf(marks);
	}

	/** The first page as a PNG, drawn from the same PDF. */
	public byte[] preview(byte[] pdf) {
		try (PDDocument document = Loader.loadPDF(pdf); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, PREVIEW_DPI, ImageType.RGB);
			ImageIO.write(image, "png", out);
			return out.toByteArray();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private Layout layout(ResumeDocument document, Fonts fonts) {
		Layout layout = new Layout();
		String name = printable(document.name(), layout, fonts);
		for (String piece : name.isEmpty() ? List.<String>of() : wrap(name, fonts.bold, NAME_SIZE, WIDTH)) {
			layout.text(MARGIN_X, layout.line(NAME_SIZE + 4), fonts.bold, NAME_SIZE, piece);
		}
		for (String line : contact(document.contact(), layout, fonts)) {
			for (String piece : wrap(line, fonts.regular, CONTACT_SIZE, WIDTH)) {
				layout.text(MARGIN_X, layout.line(12), fonts.regular, CONTACT_SIZE, piece);
			}
		}
		for (ResumeDocument.Section section : document.sections()) {
			layout.gap(8);
			layout.room(14 + 4 + 2 * LEADING);
			float baseline = layout.line(14);
			layout.text(MARGIN_X, baseline, fonts.bold, HEADING_SIZE,
					printable(section.heading().toUpperCase(Locale.ROOT), layout, fonts));
			layout.rules.add(new Rule(layout.pages - 1, baseline - 3.5f));
			layout.gap(4);
			for (ResumeDocument.Row row : section.rows()) {
				row(row, layout, fonts);
			}
			for (ResumeDocument.Entry entry : section.entries()) {
				entry(entry, layout, fonts);
			}
		}
		return layout;
	}

	private List<String> contact(List<String> items, Layout layout, Fonts fonts) {
		List<String> lines = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String raw : items) {
			String item = printable(raw, layout, fonts);
			if (item.isEmpty()) {
				continue;
			}
			String candidate = current.isEmpty() ? item : current + SEPARATOR + item;
			if (current.isEmpty() || width(candidate, fonts.regular, CONTACT_SIZE) <= WIDTH) {
				current.setLength(0);
				current.append(candidate);
			}
			else {
				lines.add(current.toString());
				current.setLength(0);
				current.append(item);
			}
		}
		if (!current.isEmpty()) {
			lines.add(current.toString());
		}
		return lines;
	}

	private void row(ResumeDocument.Row row, Layout layout, Fonts fonts) {
		String label = printable(row.label(), layout, fonts) + ":";
		float indent = width(label + " ", fonts.bold, BODY_SIZE);
		List<String> pieces = wrap(printable(row.text(), layout, fonts), fonts.regular, BODY_SIZE, WIDTH - indent);
		for (int i = 0; i < pieces.size(); i++) {
			float baseline = layout.line(LEADING);
			if (i == 0) {
				layout.text(MARGIN_X, baseline, fonts.bold, BODY_SIZE, label);
			}
			layout.text(MARGIN_X + indent, baseline, fonts.regular, BODY_SIZE, pieces.get(i));
		}
		layout.gap(1.5f);
	}

	private void entry(ResumeDocument.Entry entry, Layout layout, Fonts fonts) {
		String title = printable(entry.title(), layout, fonts);
		String dates = printable(entry.dates(), layout, fonts);
		String subtitle = printable(entry.subtitle(), layout, fonts);
		float datesWidth = dates.isEmpty() ? 0 : width(dates, fonts.regular, BODY_SIZE);
		List<String> titles = wrap(title, fonts.bold, BODY_SIZE, WIDTH - (dates.isEmpty() ? 0 : datesWidth + DATES_GAP));
		List<String> subtitles = subtitle.isEmpty() ? List.of() : wrap(subtitle, fonts.italic, BODY_SIZE, WIDTH);
		layout.gap(3);
		layout.room(LEADING * (titles.size() + subtitles.size() + (entry.bullets().isEmpty() ? 0 : 1)));
		for (int i = 0; i < titles.size(); i++) {
			float baseline = layout.line(LEADING);
			layout.text(MARGIN_X, baseline, fonts.bold, BODY_SIZE, titles.get(i));
			if (i == 0) {
				layout.text(MARGIN_X + WIDTH - datesWidth, baseline, fonts.regular, BODY_SIZE, dates);
			}
		}
		for (String piece : subtitles) {
			layout.text(MARGIN_X, layout.line(LEADING), fonts.italic, BODY_SIZE, piece);
		}
		for (String bullet : entry.bullets()) {
			List<String> pieces = wrap(printable(bullet, layout, fonts), fonts.regular, BODY_SIZE, WIDTH - TEXT_X);
			for (int i = 0; i < pieces.size(); i++) {
				float baseline = layout.line(LEADING);
				if (i == 0) {
					layout.mark(MARGIN_X + BULLET_X, baseline, fonts.regular, BODY_SIZE, "\u2022");
				}
				layout.text(MARGIN_X + TEXT_X, baseline, fonts.regular, BODY_SIZE, pieces.get(i));
			}
		}
	}

	/** Breaks at spaces; a word wider than the line is split between characters, never hyphenated. */
	private static List<String> wrap(String text, PDType1Font font, float size, float width) {
		List<String> pieces = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String word : text.split(" ")) {
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

	private static float width(String text, PDType1Font font, float size) {
		try {
			return font.getStringWidth(text) / 1000 * size;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/** The text as it will print: one line, plain spacing, and "?" for any character the font cannot draw. */
	private static String printable(String text, Layout layout, Fonts fonts) {
		StringBuilder out = new StringBuilder(text.length());
		text.codePoints().forEach(point -> {
			String plain = PLAIN.get(point);
			if (plain != null) {
				out.append(plain);
			}
			else if (Character.isWhitespace(point) || Character.isISOControl(point)) {
				out.append(' ');
			}
			else if (prints(point, fonts)) {
				out.appendCodePoint(point);
			}
			else {
				layout.unprintable.add(new String(Character.toChars(point)));
				out.append('?');
			}
		});
		return out.toString().replaceAll(" {2,}", " ").strip();
	}

	private static boolean prints(int point, Fonts fonts) {
		return PRINTS.computeIfAbsent(point, key -> {
			String character = new String(Character.toChars(key));
			try {
				fonts.regular.encode(character);
				fonts.bold.encode(character);
				fonts.italic.encode(character);
				return true;
			}
			catch (IOException | IllegalArgumentException ex) {
				return false;
			}
		});
	}

	private List<Box> firstPageBoxes(Layout layout, Fonts fonts, String line) {
		Layout scratch = new Layout();
		String print = printable(line, scratch, fonts);
		if (print.isEmpty()) {
			return List.of();
		}
		List<String> pieces = wrap(print, fonts.regular, BODY_SIZE, WIDTH - TEXT_X);
		List<Op> body = new ArrayList<>();
		for (Op op : layout.ops) {
			if (op.page() == 0 && op.size() == BODY_SIZE && !"\u2022".equals(op.text())) {
				body.add(op);
			}
		}
		for (int i = 0; i <= body.size() - pieces.size(); i++) {
			boolean match = true;
			for (int j = 0; j < pieces.size(); j++) {
				if (!pieces.get(j).equals(body.get(i + j).text())) {
					match = false;
					break;
				}
			}
			if (match) {
				List<Box> boxes = new ArrayList<>();
				for (int j = 0; j < pieces.size(); j++) {
					boxes.add(box(body.get(i + j)));
				}
				return List.copyOf(boxes);
			}
		}
		return List.of();
	}

	private static Box box(Op op) {
		float scale = PREVIEW_DPI / 72f;
		float width;
		try {
			width = op.font().getStringWidth(op.text()) / 1000 * op.size();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		float height = op.size() * 1.28f;
		float pad = 2.5f * scale;
		float x = op.x() * scale - pad;
		float y = (PAGE.getHeight() - (op.y() + 0.82f * op.size())) * scale - pad;
		return new Box(Math.max(0, x), Math.max(0, y), width * scale + 2 * pad, height * scale + 2 * pad);
	}

	/** A scored bullet and where it sits on the first-page PNG. */
	public record Mark(String text, List<Box> boxes) {
	}

	/** Pixel box on the preview PNG, origin at the top left. */
	public record Box(float x, float y, float width, float height) {
	}

	private record Op(int page, float x, float y, PDType1Font font, float size, String text) {
	}

	private record Rule(int page, float y) {
	}

	private static final class Fonts {

		private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

		private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

		private final PDType1Font italic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

	}

	private static final class Layout {

		private final List<Op> ops = new ArrayList<>();

		private final List<Rule> rules = new ArrayList<>();

		private final List<String> fragments = new ArrayList<>();

		private final Set<String> unprintable = new LinkedHashSet<>();

		private int pages = 1;

		private float y = PAGE.getHeight() - MARGIN_Y;

		/** Starts a new page unless {@code height} more points fit on this one. */
		private void room(float height) {
			if (y - height < MARGIN_Y) {
				pages++;
				y = PAGE.getHeight() - MARGIN_Y;
			}
		}

		/** Takes one line of the given height and returns its baseline. */
		private float line(float height) {
			room(height);
			float baseline = y - 0.78f * height;
			y -= height;
			return baseline;
		}

		private void gap(float height) {
			y -= height;
		}

		private void text(float x, float baseline, PDType1Font font, float size, String text) {
			if (!text.isEmpty()) {
				ops.add(new Op(pages - 1, x, baseline, font, size, text));
				fragments.add(text);
			}
		}

		/** Drawn but not part of the text to read back, like a bullet sign. */
		private void mark(float x, float baseline, PDType1Font font, float size, String text) {
			ops.add(new Op(pages - 1, x, baseline, font, size, text));
		}

	}

}
