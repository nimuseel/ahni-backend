# Development Commands

## Prerequisites

- Java 26
- Docker for PostgreSQL integration tests
- Gradle Wrapper included in the repository

## Commands

```bash
./scripts/setup             # resolve and report Gradle dependencies
./scripts/dev               # run the local API
./scripts/test-unit         # run tests without the integration tag
./scripts/test-integration  # run Spring and PostgreSQL boundary tests
./scripts/lint              # run the Gradle check lifecycle
./scripts/typecheck         # compile production and test sources
./scripts/verify            # clean, check, and run integration tests
```

The default Gradle `test` task and `unitTest` exclude the JUnit `integration` tag. `integrationTest` includes only that tag. `scripts/verify` is the authoritative local and CI entry point and runs integration tests once, after the default verification lifecycle.

The project uses the Gradle Wrapper. Do not require a globally installed Gradle version for development or CI. Docker is a hard prerequisite for the PostgreSQL Testcontainers test; when Docker is unavailable, `integrationTest` and `scripts/verify` fail with a clear prerequisite message.

## Supabase local run

Load the untracked `.env` file and run the Supabase profile explicitly:

```bash
set -a
source .env
set +a
./gradlew bootRun --args='--spring.profiles.active=supabase'
```

The Supabase profile runs Flyway against the configured PostgreSQL database. Never commit `.env` or print its values in logs.
