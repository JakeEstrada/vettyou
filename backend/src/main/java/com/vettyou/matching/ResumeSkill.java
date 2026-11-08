package com.vettyou.matching;

/**
 * A kept skill for a resume's skills section. {@code shown} is the posting's spelling when the posting names the same
 * thing ("Postgres" for PostgreSQL), otherwise {@code name}. {@code asked} is true when the posting names it.
 */
public record ResumeSkill(String name, String category, String shown, boolean asked, int places) {
}
