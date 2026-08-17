# AHNI Backend Review Rules

- Use Java 26 and the Gradle Wrapper.
- Keep HTTP, application, domain, and adapter responsibilities separate.
- Validate external input at the boundary and preserve authenticated ownership checks.
- Add unit tests for pure rules and integration tests for Spring boundaries.
- Run `./scripts/verify` before considering a change complete.
- 커밋·푸시 전에는 `docs/development/git-workflow.md`를 따르고, 사용자 요청 없이 자동 커밋·푸시하지 않습니다.
