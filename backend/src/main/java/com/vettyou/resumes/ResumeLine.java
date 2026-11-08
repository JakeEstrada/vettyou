package com.vettyou.resumes;

import java.util.List;
import java.util.UUID;

import com.vettyou.matching.LineBacking;
import com.vettyou.matching.RankedLine;

/**
 * One of the person's lines and what it does for this posting. {@code gain} is the share of the posting it adds that
 * better lines do not. {@code reason} is empty for lines on the resume; otherwise {@code space}, {@code not_needed},
 * {@code too_long}, or {@code confirm}.
 */
public record ResumeLine(String kind, UUID sourceId, String label, String text, double gain, List<LineBacking> backs,
		String reason) {

	static ResumeLine of(RankedLine line, String reason) {
		return new ResumeLine(line.kind(), line.sourceId(), line.label(), line.text(), line.gain(), line.backs(),
				reason);
	}

}
