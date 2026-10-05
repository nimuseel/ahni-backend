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

`scripts/dev` loads the untracked `.env` file and runs the Supabase profile:

```bash
./scripts/dev
```

The Supabase profile runs Flyway against the configured PostgreSQL database. Never commit `.env` or print its values in logs.

## Local Docker database

The `dev` profile uses the local PostgreSQL connection defined by `AHNI_DEV_DB_URL`, `AHNI_DEV_DB_USERNAME`, and `AHNI_DEV_DB_PASSWORD`. Hibernate uses `ddl-auto=update` to create missing tables and update the schema without recreating existing tables at startup. Flyway remains disabled for this profile; the Supabase profile continues to use migrations.

Restarting the backend preserves local academic records and administrator profiles. The Docker database also uses the named `postgres_data` volume, which survives ordinary container stops and starts. Do not use `docker compose down -v` or remove that volume when you need to keep its data.

Automatic schema updates are for local development only and do not replace reviewed migrations or backups. Previously erased data cannot be recovered by changing this setting. The restart-preservation regression test runs with isolated PostgreSQL, not the developer's database:

```bash
./gradlew integrationTest --tests '*DevelopmentDatabasePersistenceIntegrationTest'
```
