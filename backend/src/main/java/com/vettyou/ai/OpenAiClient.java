package com.vettyou.ai;

import java.net.http.HttpClient;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * One structured-output call to OpenAI Chat Completions (ADR 0003). Returns the model's JSON; callers must validate
 * it before saving anything.
 */
@Component
public class OpenAiClient {

	private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

	private final String apiKey;

	private final String model;

	private final RestClient http;

	private final JsonMapper json;

	public OpenAiClient(@Value("${vettyou.ai.api-key:}") String apiKey, @Value("${vettyou.ai.model:}") String model,
			@Value("${vettyou.ai.base-url:https://api.openai.com/v1}") String baseUrl,
			@Value("${vettyou.ai.timeout-seconds:120}") int timeoutSeconds, JsonMapper json) {
		this.apiKey = apiKey == null ? "" : apiKey.strip();
		this.model = model == null ? "" : model.strip();
		this.json = json;
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
		factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
		this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
	}

	public boolean enabled() {
		return !apiKey.isEmpty() && !model.isEmpty();
	}

	public String model() {
		return model;
	}

	public JsonNode structured(String system, String user, String schemaName, JsonNode schema) {
		if (!enabled()) {
			throw new AiException("No AI key or model is configured.");
		}
		ObjectNode body = json.createObjectNode();
		body.put("model", model);
		ArrayNode messages = body.putArray("messages");
		messages.addObject().put("role", "system").put("content", system);
		messages.addObject().put("role", "user").put("content", user);
		ObjectNode format = body.putObject("response_format");
		format.put("type", "json_schema");
		ObjectNode jsonSchema = format.putObject("json_schema");
		jsonSchema.put("name", schemaName);
		jsonSchema.put("strict", true);
		jsonSchema.set("schema", schema);

		long started = System.nanoTime();
		String response;
		try {
			response = http.post()
				.uri("/chat/completions")
				.header("Authorization", "Bearer " + apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.body(json.writeValueAsString(body))
				.retrieve()
				.body(String.class);
		}
		catch (RestClientResponseException ex) {
			log.warn("OpenAI call failed status={} model={}", ex.getStatusCode().value(), model);
			throw new AiException(ex.getStatusCode().value() == 429 ? "The AI service is busy or out of credit. Try again later."
					: "The AI service answered with an error (" + ex.getStatusCode().value() + ").");
		}
		catch (RestClientException ex) {
			log.warn("OpenAI call could not complete model={} error={}", model, ex.getClass().getSimpleName());
			throw new AiException("The AI service could not be reached.");
		}
		log.info("OpenAI call done model={} ms={}", model, (System.nanoTime() - started) / 1_000_000);

		try {
			JsonNode message = json.readTree(response).path("choices").path(0).path("message");
			if (message.path("refusal").isString() && !message.path("refusal").asString().isBlank()) {
				throw new AiException("The AI service declined to read this posting.");
			}
			JsonNode content = message.path("content");
			if (!content.isString() || content.asString().isBlank()) {
				throw new AiException("The AI service returned nothing.");
			}
			return json.readTree(content.asString());
		}
		catch (JacksonException ex) {
			throw new AiException("The AI service returned something that is not valid JSON.");
		}
	}

	public static class AiException extends RuntimeException {

		public AiException(String message) {
			super(message);
		}

	}

}
