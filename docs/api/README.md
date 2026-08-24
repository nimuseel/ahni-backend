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
4. Regenerate and review the checked-in contract when the API contract guard is available.
5. Run `./scripts/verify` before requesting review.

Task 3 of the harness foundation adds the generated-contract drift guard. Until then, this guide is the canonical authoring rule.
