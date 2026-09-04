package com.ahni.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;
import java.util.List;

final class BackendArchitectureRules {

	private BackendArchitectureRules() {
	}

	static List<ArchRule> all() {
		return List.of(
			forbid("..controller..", "..repository..", "..entity..",
				"jakarta.persistence..", "org.springframework.data..",
				"org.springframework.jdbc..", "java.sql..", "javax.sql.."),
			forbid("..service..", "..controller.."),
			forbid("..repository..", "..controller..", "..service.."),
			forbid("..entity..", "..controller..", "..service..", "..repository..", "..dto.."),
			forbid("..dto..", "..controller..", "..service..", "..repository..", "..entity..",
				"jakarta.persistence..")
		);
	}

	private static ArchRule forbid(String sourcePackage, String... targetPackages) {
		return noClasses()
			.that().resideInAPackage(sourcePackage)
			.should().dependOnClassesThat().resideInAnyPackage(targetPackages)
			// Feature packages are created on demand; fixture tests exercise each boundary.
			.allowEmptyShould(true);
	}
}
