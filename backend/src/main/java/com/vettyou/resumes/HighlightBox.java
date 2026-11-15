package com.vettyou.resumes;

/**
 * A rectangle on the first-page PNG preview, in pixels, origin at the top left. Matches the 100 DPI preview of the
 * saved PDF.
 */
public record HighlightBox(double x, double y, double width, double height) {
}
