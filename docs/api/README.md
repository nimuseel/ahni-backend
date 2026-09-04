# API Authoring Guide

The backend OpenAPI document is the single API contract for the mobile and administrator clients. Clients consume a pinned contract version and must not access Supabase PostgreSQL directly.

## API completion checklist

An API change is incomplete unless the same change includes all of the following:

1. Endpoint implementation.
2. Relevant unit and integration tests.
3. OpenAPI request, response, and example updates.
4. Authentication and authorization requirements.
5. Stable error codes and failure examples.

Document successful and failure responses in OpenAPI, including the shared error shape in [`../reliability/errors.md`](../reliability/errors.md). Pull requests state whether the API change is backward compatible and which clients are affected.

## Contract workflow

1. Implement the endpoint and its authorization boundary.
2. Add the relevant unit and integration tests.
3. Update the OpenAPI request, response, and examples, including authentication requirements and stable failure codes.
4. Run `./gradlew integrationTest --tests '*OpenApiContractTest'`; if the contract intentionally changed, review `build/openapi/openapi.json` and update `docs/api/openapi.json`, then rerun the test.
5. Run `./scripts/verify` before requesting review.

`config.OpenApiContractTest` compares the generated runtime contract with the checked-in JSON semantically and fails on drift. Controllers use DTOs rather than exposing JPA entities.
