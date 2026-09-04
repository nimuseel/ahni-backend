package com.ahni.backend.dto.fixture;

import com.ahni.backend.controller.fixture.ControllerFixture;
import com.ahni.backend.entity.fixture.EntityFixture;
import com.ahni.backend.repository.fixture.RepositoryFixture;
import com.ahni.backend.service.fixture.ServiceFixture;
import jakarta.persistence.EntityManager;

public record DtoFixture(String name) {

	public record ControllerDependency(ControllerFixture controller) { }
	public record ServiceDependency(ServiceFixture service) { }
	public record RepositoryDependency(RepositoryFixture repository) { }
	public record EntityDependency(EntityFixture entity) { }
	public record JpaDependency(EntityManager entityManager) { }
}
