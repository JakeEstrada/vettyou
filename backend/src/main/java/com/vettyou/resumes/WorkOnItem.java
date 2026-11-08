package com.vettyou.resumes;

/**
 * Something the posting asks for that no resume can show from the person's current data. {@code part} is empty when
 * the whole requirement is meant. {@code status} is {@code not_shown}, {@code changed}, {@code gap}, or
 * {@code unclear}.
 */
public record WorkOnItem(String requirement, String part, String group, String status, String reason) {
}
