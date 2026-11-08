package com.vettyou.matching;

import java.util.List;
import java.util.UUID;

/**
 * A project whose lines show part of a posting. {@code share} is how much of the posting those lines show on their own,
 * even where the person's jobs show the same. {@code keys} are those lines in the order a resume should use them: the
 * one that shows the most, then each that shows the most the ones before it do not, then the rest by what they show.
 */
public record RankedProject(UUID id, String name, double share, List<String> keys) {
}
