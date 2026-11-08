package com.vettyou.matching;

import java.util.UUID;

/**
 * A cited line as shown to the person. {@code id} is the job, project, or school to link to; {@code confirmed} is false
 * for project lines the person has not kept yet.
 */
public record EvidenceCite(String kind, UUID id, String label, String excerpt, String line, boolean confirmed) {
}
