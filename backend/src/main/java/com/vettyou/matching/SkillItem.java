package com.vettyou.matching;

import java.util.List;

/**
 * A skill across everything the person has. {@code status} is {@code kept} (kept on a project or written in a job),
 * {@code review} (only found in project lines not checked yet), or {@code rejected}. {@code places} counts the
 * projects and jobs that back a kept skill.
 */
public record SkillItem(String name, String category, String status, int places, List<SkillUse> uses) {
}
