package com.ahni.backend.architecture;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ahni.backend.adapters.fixture.AdapterFixture;
import com.ahni.backend.api.fixture.ApiDependentOnAdapterType;
import com.ahni.backend.domain.fixture.HttpDependentDomainType;
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
	void domainRuleRejectsAnHttpDependency() {
		var classes = new ClassFileImporter().importClasses(HttpDependentDomainType.class);

		assertThrows(
			AssertionError.class,
			() -> BackendArchitectureRules.domainIndependence().check(classes)
		);
	}

	@Test
	void apiRuleRejectsAnAdapterDependency() {
		var classes = new ClassFileImporter().importClasses(
			ApiDependentOnAdapterType.class,
			AdapterFixture.class
		);

		assertThrows(
			AssertionError.class,
			() -> BackendArchitectureRules.apiIsolation().check(classes)
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
