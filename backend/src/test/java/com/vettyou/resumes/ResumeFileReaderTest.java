package com.vettyou.resumes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class ResumeFileReaderTest {

	private final ResumeFileReader reader = new ResumeFileReader();

	@Test
	void pdfBytesBecomeTheWordsOnThePage() throws Exception {
		byte[] pdf;
		try (PDDocument document = new PDDocument()) {
			document.addPage(new PDPage());
			try (PDPageContentStream content = new PDPageContentStream(document, document.getPage(0))) {
				content.beginText();
				content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				content.newLineAtOffset(72, 720);
				content.showText("Jane Estrada");
				content.endText();
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.save(out);
			pdf = out.toByteArray();
		}

		String text = reader.text(new ResumeUploadRequest("Jane Resume.PDF", Base64.getEncoder().encodeToString(pdf), "base64"));

		assertTrue(text.contains("Jane Estrada"));
	}

}
