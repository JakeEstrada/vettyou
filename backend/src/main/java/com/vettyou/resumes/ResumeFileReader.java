package com.vettyou.resumes;

import java.io.IOException;
import java.util.Base64;
import java.util.Locale;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import com.vettyou.common.InvalidFieldsException;

@Component
public class ResumeFileReader {

	private static final int MAX_TEXT = 200_000;

	private static final int MAX_PDF_BYTES = 5_000_000;

	private static final int MAX_PDF_PAGES = 20;

	public String text(ResumeUploadRequest request) {
		String name = request.fileName().trim().toLowerCase(Locale.ROOT);
		boolean pdf = name.endsWith(".pdf");
		boolean plain = name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".markdown");
		if (!pdf && !plain) {
			throw new InvalidFieldsException("fileName",
					"Upload a .txt, .md, or .pdf file. For Word, export a PDF or paste the text.");
		}
		String encoding = request.encoding() == null ? "" : request.encoding().trim().toLowerCase(Locale.ROOT);
		String text = pdf ? pdfText(request.body(), encoding) : plainText(request.body(), encoding);
		if (text.indexOf('\0') >= 0) {
			throw new InvalidFieldsException("body", "That file is not plain text.");
		}
		String trimmed = text.trim();
		if (trimmed.isEmpty()) {
			throw new InvalidFieldsException("body", "That file has no text we can copy. Paste the resume instead.");
		}
		if (trimmed.length() > MAX_TEXT) {
			throw new InvalidFieldsException("body", "That resume is too long. Keep it under 200,000 characters.");
		}
		return text;
	}

	private static String plainText(String body, String encoding) {
		if ("base64".equals(encoding)) {
			throw new InvalidFieldsException("encoding", "Paste text files as text.");
		}
		return body;
	}

	private static String pdfText(String body, String encoding) {
		if (!"base64".equals(encoding)) {
			throw new InvalidFieldsException("encoding", "A PDF has to be uploaded as a file.");
		}
		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(body.replaceAll("\\s", ""));
		} catch (IllegalArgumentException exception) {
			throw new InvalidFieldsException("body", "That PDF could not be read.");
		}
		if (bytes.length > MAX_PDF_BYTES) {
			throw new InvalidFieldsException("body", "That PDF is over 5 MB. Paste the text instead.");
		}
		try (PDDocument document = Loader.loadPDF(bytes)) {
			if (document.getNumberOfPages() > MAX_PDF_PAGES) {
				throw new InvalidFieldsException("body", "That PDF is over 20 pages. Paste the text instead.");
			}
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setSortByPosition(true);
			return stripper.getText(document);
		} catch (InvalidPasswordException exception) {
			throw new InvalidFieldsException("body", "That PDF is password protected. Paste the text instead.");
		} catch (IOException exception) {
			throw new InvalidFieldsException("body", "That PDF could not be read. Paste the text instead.");
		}
	}

}
