# API Authoring Guide

The backend OpenAPI document is the single API contract for the mobile and administrator clients. Clients consume a pinned contract version and must not access Supabase PostgreSQL directly.

The read-only [grade simulation API](grade-simulation.md) compares current and projected GPA without persisting expected grades.

[연도별 교과과정 가이드](../development/yearly-curriculum.md)는 관리자 초안·공개 API와 학생 연도별 조회, 성적 저장 검증 및 단계별 배포를 설명합니다. 신규 조회는 추가 계약이지만 성적 등록 검증은 동작 변경이므로 모바일·관리자와 함께 배포를 준비해야 합니다.

## API completion checklist

`GET /api/v1/admin/me` returns the authenticated user's active administrator profile. A valid Supabase session without an active `admin` mapping receives `403 ADMIN_ACCESS_DENIED`.

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
