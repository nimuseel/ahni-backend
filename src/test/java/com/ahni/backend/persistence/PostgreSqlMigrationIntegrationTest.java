package com.ahni.backend.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

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

	@Test
	void appliesMigrationsOneAndTwoSuccessfully() {
		Map<String, MigrationState> appliedMigrations = Arrays.stream(flyway.info().applied())
			.collect(Collectors.toMap(
				migration -> migration.getVersion().getVersion(),
				MigrationInfo::getState
			));

		assertAll(
			() -> assertEquals(MigrationState.SUCCESS, appliedMigrations.get("1")),
			() -> assertEquals(MigrationState.SUCCESS, appliedMigrations.get("2"))
		);
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
