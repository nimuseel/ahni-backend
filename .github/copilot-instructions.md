# AHNI Backend Review Rules

- Use Java 26 and the Gradle Wrapper.
- Follow Controller → Service → Repository, with JPA entities in `entity` and API contracts in `dto`; follow `ARCHITECTURE.md`.
- Reject direct controller persistence access, entity exposure in API contracts, and lower layers depending on controllers/services.
- Keep business rules and transactions in services. Do not require ports/adapters or empty interface/implementation pairs.
- Validate external input at the boundary and preserve authenticated ownership checks.
- Add unit tests for pure rules and integration tests for Spring boundaries.
- Treat an API change as incomplete until endpoint implementation, relevant unit and integration tests, OpenAPI request, response, and example updates, authentication and authorization requirements, and stable error codes and failure examples are present.
- Run `./scripts/verify` before considering a change complete.
- 커밋·푸시 전에는 `docs/development/git-workflow.md`를 따르고, 사용자 요청 없이 자동 커밋·푸시하지 않습니다.
