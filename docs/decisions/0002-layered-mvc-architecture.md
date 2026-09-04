# Decision 0002: layered Spring MVC REST architecture

Status: Accepted, 2026-09-04.

## Context

The initial harness introduced api/application/domain/ports/adapters markers and dependency rules, but no business endpoints. The approved implementation now uses a conventional layered Spring Boot structure to keep early development and JPA usage straightforward.

## Decision

Use Controller → Service → Repository, with separate entity, dto, config, and exception responsibilities as defined in [ARCHITECTURE.md](../../ARCHITECTURE.md). Services may depend directly on Spring Data repositories. JPA entities may use persistence annotations; HTTP contracts use DTOs.

Keep a single application and JSON API for Flutter and React. Do not introduce server-rendered views, mandatory service interfaces, or empty package markers. Create each feature package with its first real implementation.

## Alternatives

Retaining ports/adapters would add abstraction before concrete integrations need it. A dedicated client or interface can be introduced later where multiple implementations or an external boundary justify it.

## Consequences

- Replace old ArchUnit restrictions with layered dependency checks and positive/negative fixture tests.
- Move OpenAPI configuration to config without changing the generated API contract.
- Preserve authentication, verification, database settings, migrations, and API documentation requirements. This decision changes code organization only.
- This decision supersedes backend package instructions in the 2026-08-24 harness design/plan and the application-port requirement in Decision 0001; their unrelated product and security decisions remain in effect.
