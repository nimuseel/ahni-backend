# Testing Guide

## Layers

- Unit: service business rules, entity behavior, and pure calculations. Fast, no Spring context; mock repositories or external clients when needed.
- Integration: controller HTTP contracts, validation, authorization, serialization, JPA persistence, and external integration boundaries.
- Architecture: Controller → Service → Repository dependencies, controller persistence isolation, and entity/DTO separation, enforced by ArchUnit.
- E2E: API scenarios covering a user-visible outcome when the HTTP surface is available.

## Rules

Write a failing test before a new business behavior. Use real entities and DTOs where possible, keep test names behavior-focused, and run the narrowest test during development before `./scripts/verify`.

Current baseline: context startup, OpenAPI drift, PostgreSQL migrations, migration-summary unit tests, and ArchUnit rules. `LayerDependencyTest` tests forbidden edges and permitted service/repository/JPA dependencies with non-component fixtures, then checks production classes only. Empty feature packages are allowed because they are created on demand, not filled with markers.

Run `./gradlew test --tests '*LayerDependencyTest'` for architecture changes and `./gradlew integrationTest --tests '*OpenApiContractTest'` for the API contract. New controller/service/repository behavior gets its own tests with the corresponding feature; architecture fixtures are not business implementations.

## API completion rule

Every API change includes all of the following:

1. Endpoint implementation.
2. Relevant unit and integration tests.
3. OpenAPI request, response, and example updates.
4. Authentication and authorization requirements.
5. Stable error codes and failure examples.

The OpenAPI contract must cover successful and documented failure responses. Tests protect behavior; they do not verify documentation by grepping its wording.
