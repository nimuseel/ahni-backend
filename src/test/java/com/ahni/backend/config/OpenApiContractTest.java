package com.ahni.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
class OpenApiContractTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void generatedContractMatchesCheckedInContract() throws Exception {
		String actual = mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		Path generated = Path.of("build/openapi/openapi.json");
		Files.createDirectories(generated.getParent());
		Files.writeString(generated, actual);
		Path expected = Path.of("docs/api/openapi.json");
		assertTrue(Files.exists(expected), "copy build/openapi/openapi.json to docs/api/openapi.json");
		assertEquals(objectMapper.readTree(Files.readString(expected)), objectMapper.readTree(actual));
	}

	@Test
	void swaggerUiIsAvailable() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/html"));
	}

}
