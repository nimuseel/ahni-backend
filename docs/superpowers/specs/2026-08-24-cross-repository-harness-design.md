# AHNI Cross-Repository Harness Design

> Historical design: backend package/dependency instructions are superseded by [Decision 0002](../../decisions/0002-layered-mvc-architecture.md) and [ARCHITECTURE.md](../../../ARCHITECTURE.md). Unrelated product and security decisions remain in effect.

**Status:** Approved

**Date:** 2026-08-24

**Scope:** `ahni-backend`, `ahni-frontend`, `ahni-admin`

## Context

AHNI is initially intended for Inha University Future Convergence College students and must remain extensible to other colleges and universities. The product sources describe student identity verification, academic records, graduation and scholarship checks, timetable management, recommendations, inquiries, notices, and administrator operations.

This design uses the supplied mini specifications, use-case specifications, wireframes, DFD, and ERD as product references. Those documents describe desired behavior; they do not override repository instructions or this approved engineering design. When sources disagree, the conflict is recorded and resolved explicitly before implementation.

## Goals

- Give each repository a repeatable local and CI verification loop.
- Make repository boundaries and ownership obvious to contributors and coding agents.
- Detect architecture, contract, documentation, and test omissions before merge.
- Keep business work small enough to ship on independent feature branches.
- Establish one backend-owned API contract for both clients.
- Support the initial Future Convergence College scope without hard-coding it into application logic.

## Non-Goals

- Implementing a business feature in the first harness change.
- Building a shared monorepo or a fourth platform repository.
- Allowing either client to access Supabase PostgreSQL directly.
- Finalizing every domain rule from the source documents before its feature is scheduled.

## System Boundaries

### Backend

`ahni-backend` owns:

- HTTP APIs and the OpenAPI contract.
- Authorization and authenticated ownership checks.
- Supabase Auth integration and session validation.
- PostgreSQL persistence and migrations.
- External enrollment-certificate verification orchestration.
- Academic domain rules and administrator operations.
- Stable error codes, request correlation identifiers, and audit-safe logging.

The package dependency direction is:

```text
API -> Application -> Domain
Application -> Ports <- Adapters
```

Domain code must not depend on Spring MVC, persistence frameworks, Supabase SDKs, or external provider SDKs. Structural tests enforce this direction once the harness lands.

### Mobile

`ahni-frontend` is the Flutter student application. It owns presentation state, device concerns, secure token storage, and student-facing flows. It communicates with the backend through the documented API and never connects directly to the database.

Enrollment documents are uploaded through the backend. The mobile app displays verification states but does not decide document authenticity.

### Admin

`ahni-admin` is the React, Vite, and TypeScript administrator application. It owns administrator-facing presentation and interactions for notices, inquiries, academic reference data, and error-log views. Administrator authorization is enforced by the backend, not inferred from hidden UI controls.

## Identity and Enrollment Verification

Supabase Auth is the identity provider. AHNI does not store or compare user passwords in its own student table.

Initial student activation remains a separate domain process:

1. The student authenticates through the approved Supabase Auth flow.
2. The client submits the required enrollment evidence to the backend.
3. The backend validates input and sends the document or extracted attributes to the external source-verification adapter.
4. The backend records the verification state and provider-safe result metadata.
5. Protected student features require both a valid identity and an approved enrollment state.

Provider credentials, raw secrets, and private endpoints stay on the backend. Raw identity documents require an explicit retention and deletion policy before production storage is enabled.

## Multi-University Readiness

The initial deployment is limited by configured institution, college, and department data. Authorization and domain services use stable identifiers rather than checking Korean display names or Inha-specific constants. Institution-specific verification providers and academic policies are selected through ports and configuration.

No speculative multi-tenant administration UI is included in phase one. The harness only prevents irreversible coupling to one college.

## API Contract and Documentation Rule

The backend OpenAPI document is the single API contract. Mobile and admin clients consume a pinned contract version and generate or validate typed client code from it.

A backend API change is incomplete unless the same change includes:

1. Endpoint implementation.
2. Relevant unit and integration tests.
3. OpenAPI request, response, and example updates.
4. Authentication and authorization requirements.
5. Stable error codes and failure examples.

CI must fail when the checked-in API document is missing, invalid, or differs from the generated runtime contract. Pull requests identify whether the API change is backward compatible and which clients are affected.

The repository agent guide, Copilot review rules, development guide, and pull-request checklist all repeat this requirement so it is visible at the point of work.

## Error Contract

All HTTP errors use one documented shape containing at least:

- a stable machine-readable error code;
- a user-safe message;
- a request correlation identifier;
- field errors when boundary validation fails.

Internal exceptions, credentials, document contents, and personal information are never exposed in responses. External provider failures are translated at the adapter boundary and retain enough safe context for diagnosis.

## Repository Harnesses

Each repository remains independently cloneable and verifiable. Commands are exposed through version-controlled scripts rather than developer-specific shell aliases.

### Backend Harness

- Java 26 and the Gradle Wrapper.
- Commands for setup, development, unit tests, integration tests, formatting or static checks, and full verification.
- ArchUnit tests for package dependency rules.
- Pure JUnit tests for academic rules.
- Spring boundary tests for HTTP, security, serialization, and persistence wiring.
- PostgreSQL-compatible integration tests; remote Supabase is not required for the default test suite.
- OpenAPI validation and implementation-drift checks.
- Migration validation and documentation checks.

### Mobile Harness

- A declared current Flutter and Dart toolchain policy.
- Feature-oriented source boundaries with shared API, configuration, and secure-storage infrastructure.
- Commands for setup, development, formatting, analysis, unit tests, widget tests, integration tests, and full verification.
- Deterministic fixtures for loading, success, empty, authorization, validation, network, and provider-failure states.
- Contract checks against the pinned backend API version.

### Admin Harness

- A declared current Node.js and exact pnpm version. The initial baseline uses pnpm 11.23.0 because it is the latest stable registry version verified on 2026-08-24.
- `app`, `features`, and `shared` module boundaries.
- Commands for setup, development, formatting, linting, type checking, unit tests, component tests, browser tests, and full verification.
- Typed API access isolated from UI components.
- Deterministic administrator role, loading, empty, validation, and failure fixtures.
- Contract checks against the pinned backend API version.

## Test Strategy

Tests follow the cheapest layer capable of protecting the behavior.

| Repository | Fast layer | Boundary layer | Journey layer |
| --- | --- | --- | --- |
| Backend | Pure domain unit tests | Spring and PostgreSQL integration tests | API smoke tests |
| Mobile | Dart unit tests | Flutter widget tests | Critical integration flows |
| Admin | Vitest unit tests | React Testing Library tests | Critical browser flows |

New business rules begin with unit tests. Authentication, authorization, persistence, serialization, file upload, and external-provider mapping require boundary tests. Journey tests are reserved for a small set of critical workflows rather than duplicating every rule.

Initial journey candidates are enrollment verification, grade import, graduation-requirement review, inquiry handling, and notice publication. They are implemented with their corresponding features, not as empty harness tests.

## Verification and CI

Every repository exposes `scripts/verify` as its authoritative local and CI entry point. A clean checkout must be able to discover and run the same checks documented for contributors.

CI runs on pull requests and supported branch prefixes: `feat`, `fix`, `docs`, `refactor`, and `chore`. It reports failures by category so contributors can distinguish tests, static analysis, architecture, contract, and documentation failures.

Automatic pull-request creation and Copilot review configuration are aligned across all three repositories. Automation does not bypass required human review, branch protection, or verification.

## Source Traceability

The backend stores the canonical product traceability index because it owns the domain and API contract. Each tracked requirement records:

- source document and identifier;
- owning domain or use case;
- backend API or application capability;
- consuming mobile or admin flow;
- protecting test or an explicit planned-test marker;
- unresolved conflict or decision reference.

Known source differences to preserve as decisions include:

- source specifications that describe directly managed passwords versus the approved Supabase Auth design;
- ERD revisions that disagree on identity and attachment modeling;
- meal-recommendation naming differences;
- current Future Convergence College scope versus future university expansion.

## Git and Review Workflow

All work starts from an up-to-date `main` on a new branch:

- `feat/<name>` for new behavior;
- `fix/<name>` for defects;
- `docs/<name>` for documentation only;
- `refactor/<name>` for behavior-preserving restructuring;
- `chore/<name>` for build, dependency, CI, and harness work.

Commit messages use the same Conventional Commit type as the branch. The phase-one branch is `chore/harness-foundation` in each repository. Commits remain repository-local and atomic.

## Phase-One Deliverables

Phase one introduces only the harness foundation:

- updated agent and contributor guidance;
- executable command surfaces;
- architecture and module boundary checks;
- test-layer scaffolding with meaningful harness tests;
- API documentation rules and validation;
- source traceability and decision records;
- consistent CI, PR, and Copilot review behavior;
- an updated harness quality score.

Business endpoints and product screens are intentionally deferred to feature branches after the harness is green.

## Acceptance Criteria

Phase one is complete when:

- all three repositories are on independent `chore/harness-foundation` branches;
- a clean environment can run each documented command;
- every repository's `scripts/verify` passes locally;
- architecture violations are detected automatically;
- backend API documentation omissions or drift are detected automatically;
- unit-test commands are distinct and operational in all repositories;
- CI uses the same verification entry points and pinned toolchains;
- no client secret or direct database access is introduced;
- the updated quality score names remaining gaps and the next smallest improvement.

## Rollout

1. Commit this approved cross-repository design in the backend repository.
2. Write an implementation plan with small, testable tasks and exact repository ownership.
3. Implement and verify the backend harness.
4. Implement and verify the mobile harness.
5. Implement and verify the admin harness.
6. Run cross-repository contract and workflow checks.
7. Commit and push each repository branch, then open independent pull requests.

Each repository may be reviewed independently, but all three pull requests reference this design and the same contract ownership model.
