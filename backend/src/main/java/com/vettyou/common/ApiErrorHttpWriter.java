package com.vettyou.common;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ApiErrorHttpWriter {

	private final JsonMapper jsonMapper;

	public ApiErrorHttpWriter(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public void write(HttpServletResponse response, int status, String code, String message) throws IOException {
		write(response, ApiErrors.of(status, code, message, List.of()));
	}

	public void write(HttpServletResponse response, ApiError error) throws IOException {
		response.setStatus(error.status());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		jsonMapper.writeValue(response.getOutputStream(), error);
	}

}
