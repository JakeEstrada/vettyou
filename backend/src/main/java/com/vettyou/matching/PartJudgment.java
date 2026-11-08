package com.vettyou.matching;

import java.util.List;

/** What the matcher decided for one part. Every citation has already been found word for word in the person's data. */
public record PartJudgment(String support, String reason, List<EvidenceRef> evidence) {
}
