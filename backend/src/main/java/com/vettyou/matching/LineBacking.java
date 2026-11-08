package com.vettyou.matching;

/**
 * A requirement part that one of the person's lines backs. {@code group} is {@code required}, {@code responsibility},
 * or {@code preferred}; {@code support} is {@code direct} or {@code transferable}.
 */
public record LineBacking(String requirement, String part, String group, String support) {
}
