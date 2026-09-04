package com.ahni.backend.architecture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ahni.backend.controller.fixture.ControllerFixture;
import com.ahni.backend.dto.fixture.DtoFixture;
import com.ahni.backend.entity.fixture.EntityFixture;
import com.ahni.backend.repository.fixture.RepositoryFixture;
import com.ahni.backend.service.fixture.ServiceFixture;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class LayerDependencyTest {

	@ParameterizedTest
	@MethodSource("forbiddenDependencies")
	void rejectsForbiddenDependencies(Class<?> violatingType) {
		var classes = new ClassFileImporter().importClasses(violatingType);

		var violation = assertThrows(AssertionError.class, () ->
			BackendArchitectureRules.all().forEach(rule -> rule.check(classes))
		);
		assertTrue(violation.getMessage().contains(violatingType.getName()),
			"The failure must identify the violating class, not an empty rule selection");
	}

	@Test
	void allowsControllerServiceRepositoryFlowAndJpaEntities() {
		var classes = new ClassFileImporter().importClasses(
			ControllerFixture.class, ServiceFixture.class, RepositoryFixture.class,
			EntityFixture.class, DtoFixture.class
		);

		BackendArchitectureRules.all().forEach(rule -> rule.check(classes));
	}

	@Test
	void productionPackagesSatisfyLayerBoundaries() {
		var classes = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages("com.ahni.backend");

		BackendArchitectureRules.all().forEach(rule -> rule.check(classes));
	}

	private static Stream<Class<?>> forbiddenDependencies() {
		return Stream.of(
			ControllerFixture.RepositoryDependency.class,
			ControllerFixture.EntityDependency.class,
			ControllerFixture.JpaDependency.class,
			ControllerFixture.JdbcDependency.class,
			ServiceFixture.ControllerDependency.class,
			RepositoryFixture.ControllerDependency.class,
			RepositoryFixture.ServiceDependency.class,
			EntityFixture.ControllerDependency.class,
			EntityFixture.ServiceDependency.class,
			EntityFixture.RepositoryDependency.class,
			EntityFixture.DtoDependency.class,
			DtoFixture.ControllerDependency.class,
			DtoFixture.ServiceDependency.class,
			DtoFixture.RepositoryDependency.class,
			DtoFixture.EntityDependency.class,
			DtoFixture.JpaDependency.class
		);
	}
}
