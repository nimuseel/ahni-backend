package com.ahni.backend.application.fixture;

import com.ahni.backend.adapters.fixture.AdapterFixture;
import com.ahni.backend.api.fixture.ApiFixture;

public final class ApplicationForbiddenDependencies {

	private ApplicationForbiddenDependencies() {
	}

	public static class Api {
		private final ApiFixture api = new ApiFixture();
	}

	public static class Adapters {
		private final AdapterFixture adapter = new AdapterFixture();
	}
}
