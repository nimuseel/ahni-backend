package com.ahni.backend.domain.fixture;

import com.ahni.backend.adapters.fixture.AdapterFixture;
import com.ahni.backend.api.fixture.ApiFixture;
import com.ahni.backend.application.fixture.ApplicationFixture;
import com.ahni.backend.ports.fixture.PortFixture;

public final class DomainForbiddenDependencies {

	private DomainForbiddenDependencies() {
	}

	public static class Api {
		private final ApiFixture api = new ApiFixture();
	}

	public static class Application {
		private final ApplicationFixture application = new ApplicationFixture();
	}

	public static class Ports {
		private final PortFixture port = new PortFixture();
	}

	public static class Adapters {
		private final AdapterFixture adapter = new AdapterFixture();
	}
}
