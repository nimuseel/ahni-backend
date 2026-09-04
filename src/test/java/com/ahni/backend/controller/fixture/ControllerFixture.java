package com.ahni.backend.controller.fixture;

import com.ahni.backend.dto.fixture.DtoFixture;
import com.ahni.backend.entity.fixture.EntityFixture;
import com.ahni.backend.repository.fixture.RepositoryFixture;
import com.ahni.backend.service.fixture.ServiceFixture;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

public record ControllerFixture(ServiceFixture service, DtoFixture response) {

	public record RepositoryDependency(RepositoryFixture repository) { }
	public record EntityDependency(EntityFixture entity) { }
	public record JpaDependency(EntityManager entityManager) { }
	public record JdbcDependency(JdbcTemplate jdbcTemplate) { }
}
