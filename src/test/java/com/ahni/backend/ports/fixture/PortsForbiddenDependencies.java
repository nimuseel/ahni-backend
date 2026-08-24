package com.ahni.backend.ports.fixture;

import com.ahni.backend.adapters.fixture.AdapterFixture;
import com.ahni.backend.api.fixture.ApiFixture;
import com.ahni.backend.application.fixture.ApplicationFixture;
import org.springframework.stereotype.Component;

public final class PortsForbiddenDependencies {

	private PortsForbiddenDependencies() {
	}

	public static class Api {
		private final ApiFixture api = new ApiFixture();
	}

	public static class Application {
		private final ApplicationFixture application = new ApplicationFixture();
	}

	public static class Adapters {
		private final AdapterFixture adapter = new AdapterFixture();
	}

	@Component
	public static class Framework {
	}
}
