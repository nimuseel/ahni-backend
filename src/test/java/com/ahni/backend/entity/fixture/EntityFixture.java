package com.ahni.backend.entity.fixture;

import com.ahni.backend.controller.fixture.ControllerFixture;
import com.ahni.backend.dto.fixture.DtoFixture;
import com.ahni.backend.repository.fixture.RepositoryFixture;
import com.ahni.backend.service.fixture.ServiceFixture;
import jakarta.persistence.Column;

public class EntityFixture {

	@Column
	private String name;

	public record ControllerDependency(ControllerFixture controller) { }
	public record ServiceDependency(ServiceFixture service) { }
	public record RepositoryDependency(RepositoryFixture repository) { }
	public record DtoDependency(DtoFixture dto) { }
}
