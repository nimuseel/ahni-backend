package com.ahni.backend.architecture;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ahni.backend.domain.fixture.ViolatingDomainType;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class LayerDependencyTest {

	@Test
	void domainRuleRejectsAFrameworkDependency() {
		var classes = new ClassFileImporter().importClasses(ViolatingDomainType.class);

		assertThrows(
			AssertionError.class,
			() -> BackendArchitectureRules.domainIndependence().check(classes)
		);
	}

	@Test
	void productionPackagesSatisfyLayerBoundaries() {
		var classes = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages("com.ahni.backend");

		BackendArchitectureRules.domainIndependence().check(classes);
		BackendArchitectureRules.apiIsolation().check(classes);
	}
}
