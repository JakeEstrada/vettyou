package com.vettyou.common;

import org.slf4j.MDC;

public final class CorrelationIds {

	public static final String HEADER = "X-Correlation-Id";

	public static final String MDC_KEY = "correlationId";

	private CorrelationIds() {
	}

	public static String current() {
		String id = MDC.get(MDC_KEY);
		return id == null || id.isBlank() ? "unknown" : id;
	}

}
