# AHNI Backend Agent Guide

## Mission

이 저장소는 AHNI의 Spring Boot API입니다. 학생 학사 데이터와 관리자 기능의 비즈니스 규칙을 소유하며, 모바일 앱과 관리자 웹은 이 API를 소비합니다.

## Repository Map

```text
src/main/java/com/ahni/backend/  application entry point and layered packages
src/main/resources/              runtime configuration
src/test/java/                   unit and Spring integration tests
docs/                            architecture, domain, development, reliability
scripts/                         repeatable verification commands
```

## Where to Look

| Task | Read first |
| --- | --- |
| Add an API | `ARCHITECTURE.md`, `docs/api/README.md`, `docs/development/commands.md` |
| Change academic rules | `docs/domain/index.md`, `docs/development/testing.md` |
| Change authentication or permissions | `docs/security/README.md`, `ARCHITECTURE.md` |
| Diagnose a failure | `docs/reliability/errors.md`, `docs/development/commands.md` |

## Commands

```bash
./gradlew test
./gradlew bootRun
./scripts/verify
```

## Non-Negotiable Invariants

- Follow Controller → Service → Repository. Controllers never access persistence directly or expose JPA entities.
- Services own business rules and transaction boundaries; JPA entities belong in `entity`, API contracts in `dto`.
- Follow the dependency rules in `ARCHITECTURE.md`; create packages only when their first implementation is needed.
- External input is parsed and validated at the API boundary.
- Student data access is scoped to the authenticated student or an authorized administrator.
- Every public behavior change has a unit or integration test.
- Every API change includes endpoint implementation, relevant unit and integration tests, OpenAPI request, response, and example updates, authentication and authorization requirements, and stable error codes and failure examples.
- `./scripts/verify` must pass before a change is considered complete.

## Definition of Done

Relevant tests pass, `./scripts/verify` passes, architecture or domain documentation is updated when behavior changes, and the diff has been reviewed for authorization, error handling, and data leakage. API changes are complete only when implementation, unit/integration tests, OpenAPI requests/responses/examples, auth requirements, and stable errors are all updated.
- 커밋 또는 PR 작업 | `docs/development/git-workflow.md`
