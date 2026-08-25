package com.ahni.backend.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

class MigrationSummaryTest {

	@Test
	void versionedStatesExcludeRepeatableMigrations() {
		var summary = MigrationSummary.from(List.of(
			new MigrationResult(MigrationVersion.fromVersion("1"), MigrationState.SUCCESS),
			new MigrationResult(null, MigrationState.SUCCESS)
		));

		assertEquals(Map.of("1", MigrationState.SUCCESS), summary.versionedStates());
	}

	@Test
	void allAppliedSuccessfullyIncludesRepeatableMigrations() {
		var summary = MigrationSummary.from(List.of(
			new MigrationResult(MigrationVersion.fromVersion("1"), MigrationState.SUCCESS),
			new MigrationResult(null, MigrationState.FAILED)
		));

		assertFalse(summary.allAppliedSuccessfully());
	}
}
