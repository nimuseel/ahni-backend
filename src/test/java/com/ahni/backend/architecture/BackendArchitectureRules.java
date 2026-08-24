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
				"java.net.http..",
				"jakarta.servlet..",
				"..api..",
				"..application..",
				"..ports..",
				"..adapters.."
			);
	}

	static ArchRule apiIsolation() {
		return noClasses()
			.that().resideInAPackage("..api..")
			.should().dependOnClassesThat().resideInAnyPackage(
				"..domain..",
				"..ports..",
				"..adapters.."
			);
	}

	static ArchRule applicationDependencies() {
		return noClasses()
			.that().resideInAPackage("..application..")
			.should().dependOnClassesThat().resideInAnyPackage("..api..", "..adapters..");
	}

	static ArchRule portsDependencies() {
		return noClasses()
			.that().resideInAPackage("..ports..")
			.should().dependOnClassesThat().resideInAnyPackage(
				"org.springframework..",
				"jakarta.persistence..",
				"java.net.http..",
				"jakarta.servlet..",
				"..api..",
				"..application..",
				"..adapters.."
			);
	}

	static ArchRule adapterDependencies() {
		return noClasses()
			.that().resideInAPackage("..adapters..")
			.should().dependOnClassesThat().resideInAnyPackage("..api..", "..application..");
	}
}
