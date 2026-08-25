package com.ahni.backend.api.fixture;

import com.ahni.backend.domain.fixture.DomainFixture;
import com.ahni.backend.ports.fixture.PortFixture;

public final class ApiBypassingApplicationDependencies {

	private ApiBypassingApplicationDependencies() {
	}

	public static class Domain {
		private final DomainFixture domain = new DomainFixture();
	}

	public static class Ports {
		private final PortFixture port = new PortFixture();
	}
}
