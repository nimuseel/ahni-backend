package com.ahni.backend.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Tag("integration")
class PostgreSqlMigrationIntegrationTest {

		private static final PostgreSQLContainer<?> POSTGRES = startPostgreSql();

		@DynamicPropertySource
		static void configurePostgreSql(DynamicPropertyRegistry registry) {
				registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
				registry.add("spring.datasource.username", POSTGRES::getUsername);
				registry.add("spring.datasource.password", POSTGRES::getPassword);
				registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
				registry.add("spring.flyway.enabled", () -> true);
		}

		@AfterAll
		static void stopPostgreSql() {
				if (POSTGRES.isRunning()) {
					POSTGRES.stop();
				}
		}

		@Autowired
		private Flyway flyway;

		@Autowired
		private JdbcTemplate jdbcTemplate;

		@Autowired
		private ObjectMapper objectMapper;

		@Test
		void appliesAllMigrationsSuccessfully() {
				var appliedMigrations = Arrays.stream(flyway.info().applied())
					.map(MigrationResult::from)
					.toList();
				var summary = MigrationSummary.from(appliedMigrations);

				assertAll(
					() -> assertEquals(MigrationState.SUCCESS, summary.versionedStates().get("1")),
					() -> assertEquals(MigrationState.SUCCESS, summary.versionedStates().get("2")),
					() -> assertEquals(MigrationState.SUCCESS, summary.versionedStates().get("3")),
					() -> assertEquals(MigrationState.SUCCESS, summary.versionedStates().get("4"))
				);
				assertTrue(
					summary.allAppliedSuccessfully(),
					"Every applied versioned and repeatable migration must be successful"
				);
		}

		@Test
		void allowsConfiguredSchoolEmailDomain() throws Exception {
				var response = invokeSignupHook("""
					{"user":{"email":"student@inha.edu"}}
					""");

				assertAll(
					() -> assertTrue(response.isObject()),
					() -> assertEquals(0, response.size())
				);
		}

		@Test
		void allowsSchoolEmailDomainIgnoringCase() throws Exception {
        var response = invokeSignupHook("""
					{"user":{"email":"student@INHA.EDU"}}
					""");

        assertAll(
            () -> assertTrue(response.isObject()),
            () -> assertEquals(0, response.size())
        );
		}

		@Test
		void rejectsUnconfiguredEmailDomain() throws Exception {
				var response = invokeSignupHook("""
					{"user":{"email":"student@gmail.com"}}
					""");

				var error = response.path("error");

				assertAll(
					() -> assertEquals(403, error.path("http_code").asInt()),
					() -> assertEquals(
						"Only approved school email domains are allowed.",
						error.path("message").asText()
					)
				);
		}

		@ParameterizedTest
		@ValueSource(strings = {
			"{\"user\":{\"email\":\"invalid-email\"}}",
			"{\"user\":{\"email\":\"student@inha.edu@evil.com\"}}",
			"{\"user\":{}}"
		})
		void rejectsMissingOrMalformedEmail(String eventJson) throws Exception {
				var response = invokeSignupHook(eventJson);
				var error = response.path("error");

				assertAll(
					() -> assertEquals(400, error.path("http_code").asInt()),
					() -> assertEquals(
						"A valid school email address is required.",
						error.path("message").asText()
					)
				);
		}

		private JsonNode invokeSignupHook(String eventJson) throws Exception {
				var response = jdbcTemplate.queryForObject(
					"""
							SELECT public.hook_restrict_signup_by_email_domain(
								CAST(? AS jsonb)
							)::text
					""",
					String.class,
					eventJson
				);

			return objectMapper.readTree(response);
		}

		private static PostgreSQLContainer<?> startPostgreSql() {
				PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");
				try {
					postgres.start();
					return postgres;
				}
				catch (RuntimeException exception) {
					throw new IllegalStateException(
						"PostgreSQL integration tests require Docker. Start Docker and rerun integrationTest.",
						exception
					);
				}
		}

}
