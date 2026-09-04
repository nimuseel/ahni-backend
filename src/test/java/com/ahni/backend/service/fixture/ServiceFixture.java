package com.ahni.backend.service.fixture;

import com.ahni.backend.controller.fixture.ControllerFixture;
import com.ahni.backend.dto.fixture.DtoFixture;
import com.ahni.backend.entity.fixture.EntityFixture;
import com.ahni.backend.repository.fixture.RepositoryFixture;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public record ServiceFixture(RepositoryFixture repository, EntityFixture entity, DtoFixture response) {

	public record ControllerDependency(ControllerFixture controller) { }
}
