package com.vettyou;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.vettyou.common.HealthResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.profiles.active=test")
@AutoConfigureTestRestTemplate
@Testcontainers
class HealthIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

	@Autowired
	private TestRestTemplate rest;

	@Autowired
	private JdbcTemplate jdbc;

	@LocalServerPort
	private int port;

	@Test
	void flywayRunsAndHealthReturnsOk() {
		Integer applied = jdbc.queryForObject(
				"select count(*) from flyway_schema_history where version = '1' and success", Integer.class);
		assertThat(applied).isEqualTo(1);

		List<String> columns = jdbc.queryForList(
				"""
						select column_name
						from information_schema.columns
						where table_schema = 'public' and table_name = 'users'
						order by ordinal_position
						""",
				String.class);
		assertThat(columns).containsExactly("id", "email", "password_hash", "created_at", "updated_at");

		ResponseEntity<HealthResponse> health = rest.getForEntity("/api/v1/health", HealthResponse.class);
		assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(health.getBody()).isEqualTo(new HealthResponse("ok"));

		ResponseEntity<String> actuator = rest.getForEntity("/actuator/health", String.class);
		assertThat(actuator.getStatusCode()).isEqualTo(HttpStatus.OK);

		ResponseEntity<String> blocked = rest.getForEntity("/api/v1/me", String.class);
		assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(blocked.getBody()).contains("\"code\":\"UNAUTHENTICATED\"");

		assertThat(port).isPositive();
	}

}
