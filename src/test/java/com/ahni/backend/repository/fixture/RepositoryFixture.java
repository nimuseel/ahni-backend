package com.ahni.backend.repository.fixture;

import com.ahni.backend.controller.fixture.ControllerFixture;
import com.ahni.backend.entity.fixture.EntityFixture;
import com.ahni.backend.service.fixture.ServiceFixture;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public record RepositoryFixture(JpaRepository<EntityFixture, UUID> repository) {

	public record ControllerDependency(ControllerFixture controller) { }
	public record ServiceDependency(ServiceFixture service) { }
}
