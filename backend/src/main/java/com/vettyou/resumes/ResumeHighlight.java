package com.vettyou.resumes;

import java.util.List;

/**
 * A printed bullet that counts toward the Vettyou estimate, and where it sits on the first-page PDF preview.
 * {@code why} names the posting parts this line shows.
 */
public record ResumeHighlight(String text, String why, List<HighlightBox> boxes) {
}
