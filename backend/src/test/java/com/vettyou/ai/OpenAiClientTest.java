package com.vettyou.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

class OpenAiClientTest {

	@Test
	void timeoutIsNotAMissingKey() {
		assertThat(OpenAiClient.reachError(new ResourceAccessException("read timed out", new HttpTimeoutException("timeout"))))
			.isEqualTo("The AI took too long to answer. Try again.");
		assertThat(OpenAiClient.reachError(new ResourceAccessException("timed out", new SocketTimeoutException("Read timed out"))))
			.isEqualTo("The AI took too long to answer. Try again.");
		assertThat(OpenAiClient.reachError(new ResourceAccessException("I/O error", new java.io.IOException("request timed out"))))
			.isEqualTo("The AI took too long to answer. Try again.");
	}

	@Test
	void otherNetworkErrorsStayGeneric() {
		assertThat(OpenAiClient.reachError(new ResourceAccessException("Connection refused")))
			.isEqualTo("The AI service could not be reached.");
	}

}
