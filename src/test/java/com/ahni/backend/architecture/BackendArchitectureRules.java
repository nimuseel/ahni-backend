package com.ahni.backend.architecture;

import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

final class BackendArchitectureRules {

	private BackendArchitectureRules() {
	}

	static ArchRule domainIndependence() {
		return noClasses()
			.that().resideInAPackage("..domain..")
			.should().dependOnClassesThat().resideInAnyPackage(
				"org.springframework..",
				"jakarta.persistence..",
				"..api..",
				"..adapters.."
			)
			.allowEmptyShould(true);
	}

	static ArchRule apiIsolation() {
		return noClasses()
			.that().resideInAPackage("..api..")
			.should().dependOnClassesThat().resideInAnyPackage("..adapters..")
			.allowEmptyShould(true);
	}
}
