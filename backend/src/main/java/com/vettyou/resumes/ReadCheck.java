package com.vettyou.resumes;

/**
 * One readability check on a resume's PDF, read back the way an applicant tracking system would read it.
 * {@code detail} says what failed and how to fix it, or is empty when the check passed.
 */
public record ReadCheck(String key, String label, boolean passed, String detail) {
}
