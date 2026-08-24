package com.ahni.backend.api.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
class OpenApiConfiguration {

	@Bean
	OpenAPI openApi() {
		return new OpenAPI()
			.info(new Info().title("AHNI API").version("v1"))
			.servers(List.of(new Server().url("/")));
	}

}
