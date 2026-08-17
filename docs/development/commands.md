# Development Commands

## Prerequisites

- Java 26
- Gradle Wrapper included in the repository

## Commands

```bash
./gradlew test       # all tests
./gradlew check      # verification lifecycle
./gradlew bootRun    # local API
./scripts/verify     # agent-facing verification entry point
```

The project uses the Gradle Wrapper. Do not require a globally installed Gradle version for development or CI.

## Supabase local run

Load the untracked `.env` file and run the Supabase profile explicitly:

```bash
set -a
source .env
set +a
./gradlew bootRun --args='--spring.profiles.active=supabase'
```

The Supabase profile runs Flyway against the configured PostgreSQL database. Never commit `.env` or print its values in logs.

## Supabase local run

Load the untracked `.env` file and run the Supabase profile explicitly:

```bash
set -a
source .env
set +a
./gradlew bootRun --args='--spring.profiles.active=supabase'
```

The Supabase profile runs Flyway against the configured PostgreSQL database. Never commit `.env` or print its values in logs.
