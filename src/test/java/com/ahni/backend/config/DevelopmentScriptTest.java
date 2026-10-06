package com.ahni.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DevelopmentScriptTest {
    @TempDir
    Path directory;

    @Test
    void loadsLocalEnvironmentAndRunsDevProfileFromAnotherDirectory() throws Exception {
        Path script = prepareScript();
        Files.writeString(directory.resolve(".env.dev"), """
            AHNI_DEV_DB_URL=jdbc:postgresql://localhost:5433/ahni_dev
            AHNI_SUPABASE_URL=https://local-auth.test.invalid
            """);

        Process process = runScript(script);
        assertThat(process.waitFor(5, TimeUnit.SECONDS)).isTrue();
        String output = new String(process.getInputStream().readAllBytes());

        assertThat(process.exitValue()).as(output).isZero();
        assertThat(output.lines().toList()).containsExactly(
            "GRADLE_STARTED",
            "jdbc:postgresql://localhost:5433/ahni_dev",
            "https://local-auth.test.invalid",
            "bootRun",
            "--args=--spring.profiles.active=dev"
        );
    }

    @Test
    void missingLocalEnvironmentDoesNotFallBackToSupabase() throws Exception {
        Process process = runScript(prepareScript());
        assertThat(process.waitFor(5, TimeUnit.SECONDS)).isTrue();
        String output = new String(process.getInputStream().readAllBytes());

        assertThat(process.exitValue()).as(output).isEqualTo(1);
        assertThat(output).contains(".env.dev").doesNotContain("GRADLE_STARTED");
    }

    private Path prepareScript() throws Exception {
        Path scripts = Files.createDirectory(directory.resolve("scripts"));
        Path script = Files.copy(Path.of("scripts/dev"), scripts.resolve("dev"));
        Files.writeString(directory.resolve(".env"), """
            AHNI_DEV_DB_URL=remote-database-must-not-be-selected
            AHNI_SUPABASE_URL=https://remote-auth.test.invalid
            """);
        Path gradle = directory.resolve("gradlew");
        Files.writeString(gradle, """
            #!/usr/bin/env bash
            printf '%s\\n' GRADLE_STARTED "$AHNI_DEV_DB_URL" "$AHNI_SUPABASE_URL" "$@"
            """);
        assertThat(gradle.toFile().setExecutable(true)).isTrue();
        return script;
    }

    private Process runScript(Path script) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("bash", script.toString())
            .directory(directory.resolve("scripts").toFile())
            .redirectErrorStream(true);
        builder.environment().remove("AHNI_DEV_DB_URL");
        builder.environment().remove("AHNI_SUPABASE_URL");
        return builder.start();
    }
}
