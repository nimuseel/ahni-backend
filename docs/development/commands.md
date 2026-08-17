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
