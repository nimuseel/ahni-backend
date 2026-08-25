package com.ahni.backend.persistence;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationVersion;

record MigrationSummary(
	Map<String, MigrationState> versionedStates,
	List<MigrationState> appliedStates
) {

	static MigrationSummary from(List<MigrationResult> migrations) {
		Map<String, MigrationState> versionedStates = migrations.stream()
			.filter(MigrationResult::isVersioned)
			.collect(Collectors.toUnmodifiableMap(
				migration -> migration.version().getVersion(),
				MigrationResult::state
			));
		List<MigrationState> appliedStates = migrations.stream()
			.map(MigrationResult::state)
			.toList();

		return new MigrationSummary(versionedStates, appliedStates);
	}

	boolean allAppliedSuccessfully() {
		return appliedStates.stream().allMatch(MigrationState.SUCCESS::equals);
	}
}

record MigrationResult(MigrationVersion version, MigrationState state) {

	static MigrationResult from(MigrationInfo migration) {
		return new MigrationResult(migration.getVersion(), migration.getState());
	}

	boolean isVersioned() {
		return version != null;
	}
}
