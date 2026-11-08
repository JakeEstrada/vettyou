package com.vettyou.matching;

import java.util.List;

/**
 * One part of a requirement after scoring. {@code status} is {@code direct}, {@code transferable},
 * {@code unconfirmed} (only in project lines not kept yet), {@code not_shown}, {@code unclear}, or {@code changed}
 * (the cited line was edited or removed). {@code weight} is this part's share in its group after overlap is shared,
 * null when it is not scored. {@code sharedBy} counts the items in the group that ask for the same concept.
 */
public record PartMatch(String text, String concept, String support, String status, String reason, Double weight,
		int sharedBy, List<EvidenceCite> evidence) {
}
