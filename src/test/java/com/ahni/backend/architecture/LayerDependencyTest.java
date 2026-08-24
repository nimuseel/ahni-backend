package com.ahni.backend.architecture;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ahni.backend.adapters.fixture.AdapterAllowedDependencies;
import com.ahni.backend.adapters.fixture.AdapterForbiddenDependencies;
import com.ahni.backend.adapters.fixture.AdapterFixture;
import com.ahni.backend.api.fixture.ApiBypassingApplicationDependencies;
import com.ahni.backend.api.fixture.ApiDependentOnAdapterType;
import com.ahni.backend.api.fixture.ApiFixture;
import com.ahni.backend.application.fixture.ApplicationAllowedDependencies;
import com.ahni.backend.application.fixture.ApplicationForbiddenDependencies;
import com.ahni.backend.application.fixture.ApplicationFixture;
import com.ahni.backend.domain.fixture.DomainFixture;
import com.ahni.backend.domain.fixture.DomainForbiddenDependencies;
import com.ahni.backend.domain.fixture.HttpDependentDomainType;
import com.ahni.backend.domain.fixture.ViolatingDomainType;
import com.ahni.backend.ports.fixture.PortFixture;
import com.ahni.backend.ports.fixture.PortsForbiddenDependencies;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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

	@ParameterizedTest
	@MethodSource("apiBypassingApplicationDependencies")
	void apiRuleRejectsDependenciesThatBypassApplication(Class<?> violatingType) {
		var classes = new ClassFileImporter().importClasses(violatingType);

		assertThrows(
			AssertionError.class,
			() -> BackendArchitectureRules.apiIsolation().check(classes)
		);
	}

	@ParameterizedTest
	@MethodSource("applicationForbiddenDependencies")
	void applicationRuleRejectsDependenciesOnOuterLayers(Class<?> violatingType) {
		var classes = new ClassFileImporter().importClasses(violatingType);

		assertThrows(
			AssertionError.class,
			() -> BackendArchitectureRules.applicationDependencies().check(classes)
		);
	}

	@ParameterizedTest
	@MethodSource("domainForbiddenLayerDependencies")
	void domainRuleRejectsDependenciesOnOtherLayers(Class<?> violatingType) {
		var classes = new ClassFileImporter().importClasses(violatingType);

		assertThrows(
			AssertionError.class,
			() -> BackendArchitectureRules.domainIndependence().check(classes)
		);
	}

	@ParameterizedTest
	@MethodSource("portsForbiddenDependencies")
	void portsRuleRejectsDependenciesOnOuterLayersAndFrameworks(Class<?> violatingType) {
		var classes = new ClassFileImporter().importClasses(violatingType);

		assertThrows(
			AssertionError.class,
			() -> BackendArchitectureRules.portsDependencies().check(classes)
		);
	}

	@ParameterizedTest
	@MethodSource("adapterForbiddenDependencies")
	void adapterRuleRejectsDependenciesOnApiAndApplication(Class<?> violatingType) {
		var classes = new ClassFileImporter().importClasses(violatingType);

		assertThrows(
			AssertionError.class,
			() -> BackendArchitectureRules.adapterDependencies().check(classes)
		);
	}

	@Test
	void applicationAndAdaptersMayDependOnPortsAndDomain() {
		var classes = new ClassFileImporter().importClasses(
			ApplicationAllowedDependencies.class,
			AdapterAllowedDependencies.class,
			DomainFixture.class,
			PortFixture.class
		);

		BackendArchitectureRules.applicationDependencies().check(classes);
		BackendArchitectureRules.adapterDependencies().check(classes);
	}

	@Test
	void productionPackagesSatisfyLayerBoundaries() {
		var classes = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages("com.ahni.backend");

		BackendArchitectureRules.domainIndependence().check(classes);
		BackendArchitectureRules.apiIsolation().check(classes);
		BackendArchitectureRules.applicationDependencies().check(classes);
		BackendArchitectureRules.portsDependencies().check(classes);
		BackendArchitectureRules.adapterDependencies().check(classes);
	}

	private static Stream<Class<?>> apiBypassingApplicationDependencies() {
		return Stream.of(
			ApiBypassingApplicationDependencies.Domain.class,
			ApiBypassingApplicationDependencies.Ports.class
		);
	}

	private static Stream<Class<?>> applicationForbiddenDependencies() {
		return Stream.of(
			ApplicationForbiddenDependencies.Api.class,
			ApplicationForbiddenDependencies.Adapters.class
		);
	}

	private static Stream<Class<?>> domainForbiddenLayerDependencies() {
		return Stream.of(
			DomainForbiddenDependencies.Api.class,
			DomainForbiddenDependencies.Application.class,
			DomainForbiddenDependencies.Ports.class,
			DomainForbiddenDependencies.Adapters.class
		);
	}

	private static Stream<Class<?>> portsForbiddenDependencies() {
		return Stream.of(
			PortsForbiddenDependencies.Api.class,
			PortsForbiddenDependencies.Application.class,
			PortsForbiddenDependencies.Adapters.class,
			PortsForbiddenDependencies.Framework.class
		);
	}

	private static Stream<Class<?>> adapterForbiddenDependencies() {
		return Stream.of(
			AdapterForbiddenDependencies.Api.class,
			AdapterForbiddenDependencies.Application.class
		);
	}
}
