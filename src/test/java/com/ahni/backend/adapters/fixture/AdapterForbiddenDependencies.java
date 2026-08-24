package com.ahni.backend.adapters.fixture;

import com.ahni.backend.api.fixture.ApiFixture;
import com.ahni.backend.application.fixture.ApplicationFixture;

public final class AdapterForbiddenDependencies {

	private AdapterForbiddenDependencies() {
	}

	public static class Api {
		private final ApiFixture api = new ApiFixture();
	}

	public static class Application {
		private final ApplicationFixture application = new ApplicationFixture();
	}
}
