package com.ahni.backend.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlMigrationIntegrationTest {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

	@DynamicPropertySource
	static void configurePostgreSql(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
		registry.add("spring.flyway.enabled", () -> true);
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

}
