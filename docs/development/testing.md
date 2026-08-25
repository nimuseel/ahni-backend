# Testing Guide

## Layers

- Unit: pure domain calculations and policy decisions. Fast, no Spring context.
- Integration: Spring boundary, serialization, persistence, and external adapter contracts.
- Architecture: dependency direction once domain/application modules exist.
- E2E: API scenarios covering a user-visible outcome when the HTTP surface is available.

## Rules

Write a failing test before a new domain behavior. Use real value objects where possible, keep test names behavior-focused, and run the narrowest test during development before `./scripts/verify`.

Current baseline: `AhniBackendApplicationTests` proves the Spring context starts. Domain unit tests are added with the first GPA and graduation-rule implementation.

## API completion rule

Every API change includes all of the following:

1. Endpoint implementation.
2. Relevant unit and integration tests.
3. OpenAPI request, response, and example updates.
4. Authentication and authorization requirements.
5. Stable error codes and failure examples.

The OpenAPI contract must cover successful and documented failure responses. Tests protect behavior; they do not verify documentation by grepping its wording.
