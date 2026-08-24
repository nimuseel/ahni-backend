package com.ahni.backend.domain.fixture;

import java.net.http.HttpClient;

public class HttpDependentDomainType {

	private final HttpClient client = HttpClient.newHttpClient();
}
