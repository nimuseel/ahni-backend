# Backend Harness Foundation Implementation Plan

> Historical plan: do not recreate its old backend packages. Package rules and OpenAPI source paths are superseded by [Decision 0002](../../decisions/0002-layered-mvc-architecture.md) and [ARCHITECTURE.md](../../../ARCHITECTURE.md).

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Spring Boot repository enforce architecture, test layers, PostgreSQL migrations, API documentation, and contributor workflow through one reproducible verification command.

**Architecture:** Keep HTTP, application, domain, ports, and adapters separate and enforce the dependency direction with ArchUnit. Generate the runtime OpenAPI document with springdoc, compare it semantically with the checked-in contract, and run documentation, unit, integration, migration, and architecture checks from Gradle.

**Tech Stack:** Java 26, Spring Boot 4.1.1, Gradle 9.5.1 Wrapper, JUnit, ArchUnit 1.5.0, springdoc 3.1.0, Testcontainers 1.21.4, PostgreSQL

**Spec:** `docs/superpowers/specs/2026-08-24-cross-repository-harness-design.md`

## Global Constraints

- Work only on `chore/harness-foundation`, created from current `origin/main`.
- Use Java 26 and the Gradle Wrapper; use stable dependency releases only.
- Clients never access Supabase PostgreSQL directly.
- Supabase Auth owns passwords; AHNI owns enrollment-verification state.
- Every future HTTP API change includes tests, OpenAPI examples, authorization requirements, and stable error codes.
- Keep commits atomic and use the `chore` Conventional Commit type.
- Run `./scripts/verify` before each final repository commit.

## File Map

- `build.gradle`: dependency versions and unit/integration task separation.
- `src/test/java/com/ahni/backend/architecture/LayerDependencyTest.java`: package dependency guard.
- `src/test/java/com/ahni/backend/api/OpenApiContractTest.java`: runtime-versus-checked-in OpenAPI drift guard.
- `src/test/java/com/ahni/backend/persistence/PostgreSqlMigrationIntegrationTest.java`: real PostgreSQL Flyway check.
- `docs/api/README.md`: API authoring and update workflow.
- `docs/api/openapi.json`: checked-in generated client contract.
- `docs/product/traceability.md`: source-to-capability decision index.
- `scripts/*`: stable developer command surface.

---

### Task 1: Make API documentation an explicit completion rule

**Files:**
- Create: `docs/api/README.md`
- Create: `docs/product/traceability.md`
- Modify: `AGENTS.md`
- Modify: `.github/copilot-instructions.md`
- Modify: `.github/pull_request_template.md`
- Modify: `docs/README.md`
- Modify: `docs/decisions/0001-identity-and-verification.md`
- Modify: `docs/development/testing.md`
- Modify: `docs/reliability/errors.md`
- Modify: `docs/security/README.md`
- Modify: `docs/exec-plans/tech-debt.md`

**Interfaces:**
- Consumes: the approved cross-repository design.
- Produces: canonical API, identity, error, and traceability guides consumed by contributors and reviewers. Task 3 provides the behavioral API drift guard.

- [ ] **Step 1: Capture the current policy conflicts**

```bash
rg -n "password|OpenAPI|correlation" AGENTS.md .github docs
```

Expected: reveal the self-managed-password decision, missing API authoring guide, and incomplete concrete error shape.

- [ ] **Step 2: Add the exact documented API completion checklist**

Add the five required items to every work surface: implementation, unit/integration tests, OpenAPI requests/responses/examples, auth requirements, and stable errors. Update Decision 0001 and the security baseline to state that Supabase Auth owns student and administrator passwords. Define the error fields `code`, `message`, `correlationId`, and `fieldErrors` in the reliability guide. Record source conflicts for passwords, ERD revisions, meal naming, and institution expansion in `docs/product/traceability.md`.

Record the existing `student.password_hash` and `admin.password_hash` columns as a blocking schema mismatch in `docs/exec-plans/tech-debt.md`. Assign their removal and replacement with Supabase identity references to a separate `fix/supabase-auth-schema` branch; do not mix that migration into the harness branch.

- [ ] **Step 3: Verify document structure and repository formatting**

Run: `test -f docs/api/README.md && test -f docs/product/traceability.md && git diff --check`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add AGENTS.md .github docs
git commit -m "chore(harness): enforce API documentation policy"
```

### Task 2: Enforce backend package boundaries

**Files:**
- Modify: `build.gradle`
- Create: `src/main/java/com/ahni/backend/api/package-info.java`
- Create: `src/main/java/com/ahni/backend/application/package-info.java`
- Create: `src/main/java/com/ahni/backend/domain/package-info.java`
- Create: `src/main/java/com/ahni/backend/ports/package-info.java`
- Create: `src/main/java/com/ahni/backend/adapters/package-info.java`
- Create: `src/test/java/com/ahni/backend/architecture/BackendArchitectureRules.java`
- Create: `src/test/java/com/ahni/backend/architecture/LayerDependencyTest.java`
- Create: `src/test/java/com/ahni/backend/domain/fixture/ViolatingDomainType.java`

**Interfaces:**
- Consumes: packages rooted at `com.ahni.backend`.
- Produces: `BackendArchitectureRules.domainIndependence()` and `BackendArchitectureRules.apiIsolation()`, plus tests that reject Spring, persistence, HTTP, and adapter dependencies from `..domain..` and reject direct API-to-adapter access.

- [ ] **Step 1: Add ArchUnit 1.5.0 and write the failing rule-factory test**

```java
@Test
void domainRuleRejectsAFrameworkDependency() {
    var classes = new ClassFileImporter().importClasses(ViolatingDomainType.class);
    assertThrows(
        AssertionError.class,
        () -> BackendArchitectureRules.domainIndependence().check(classes)
    );
}
```

`ViolatingDomainType` is a test fixture in a `..domain..` package annotated with Spring `@Component`; it never enters production sources.

- [ ] **Step 2: Verify the red architecture test**

Run: `./gradlew test --tests '*LayerDependencyTest'`

Expected: FAIL to compile because `BackendArchitectureRules` does not exist.

- [ ] **Step 3: Implement the rules and add the production-graph check**

`domainIndependence()` uses `noClasses()` for `org.springframework..`, `jakarta.persistence..`, `..api..`, and `..adapters..`. `apiIsolation()` prohibits `..api..` from directly depending on `..adapters..`. A second test checks both rules against `new ClassFileImporter().importPackages("com.ahni.backend")`. Package-info files remain documentation-only.

- [ ] **Step 4: Verify all architecture rules pass**

Run: `./gradlew test --tests '*LayerDependencyTest'`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add build.gradle src/main src/test/java/com/ahni/backend/architecture
git commit -m "chore(harness): enforce backend module boundaries"
```

### Task 3: Detect generated OpenAPI contract drift

**Files:**
- Modify: `build.gradle`
- Create: `src/main/java/com/ahni/backend/api/config/OpenApiConfiguration.java`
- Create: `src/test/java/com/ahni/backend/api/OpenApiContractTest.java`
- Create: `docs/api/openapi.json`
- Modify: `src/test/resources/application.properties`

**Interfaces:**
- Produces: runtime `GET /v3/api-docs` and a semantic comparison against `docs/api/openapi.json`.
- Consumers: mobile and admin contract snapshots.

- [ ] **Step 1: Add springdoc 3.1.0 and write the missing-contract test**

```java
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void generatedContractMatchesCheckedInContract() throws Exception {
        String actual = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        Path generated = Path.of("build/openapi/openapi.json");
        Files.createDirectories(generated.getParent());
        Files.writeString(generated, actual);
        Path expected = Path.of("docs/api/openapi.json");
        assertTrue(Files.exists(expected), "copy build/openapi/openapi.json to docs/api/openapi.json");
        assertEquals(objectMapper.readTree(Files.readString(expected)), objectMapper.readTree(actual));
    }
}
```

- [ ] **Step 2: Verify the contract test fails and emits the generated artifact**

Run: `./gradlew test --tests '*OpenApiContractTest'`

Expected: FAIL with the copy instruction and create `build/openapi/openapi.json`.

- [ ] **Step 3: Add stable API metadata and check in the generated JSON**

Configure title `AHNI API`, semantic version `v1`, and the server-relative contract. Copy the generated JSON without hand-editing.

- [ ] **Step 4: Verify semantic drift detection passes**

Run: `./gradlew test --tests '*OpenApiContractTest'`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add build.gradle src docs/api/openapi.json
git commit -m "chore(harness): verify generated OpenAPI contract"
```

### Task 4: Separate unit, integration, and migration verification

**Files:**
- Modify: `build.gradle`
- Modify: `src/test/java/com/ahni/backend/AhniBackendApplicationTests.java`
- Create: `src/test/java/com/ahni/backend/persistence/PostgreSqlMigrationIntegrationTest.java`
- Create: `scripts/setup`
- Create: `scripts/dev`
- Create: `scripts/test-unit`
- Create: `scripts/test-integration`
- Create: `scripts/lint`
- Create: `scripts/typecheck`
- Modify: `scripts/verify`
- Modify: `docs/development/commands.md`
- Modify: `.github/workflows/ci.yml`

**Interfaces:**
- Produces: Gradle tasks `unitTest`, `integrationTest`, and the authoritative `scripts/verify` workflow.

- [ ] **Step 1: Tag the existing context test as integration and add a PostgreSQL Flyway test using Testcontainers 1.21.4**

The PostgreSQL test starts `postgres:18`, maps `spring.datasource.*` through `@DynamicPropertySource`, starts the application context, and asserts that Flyway applied migrations `1` and `2` successfully.

- [ ] **Step 2: Add and run the separated Gradle tasks before wiring scripts**

Run: `./gradlew unitTest integrationTest`

Expected: PASS; `unitTest` excludes `integration`, and `integrationTest` includes it. If Docker is unavailable, the PostgreSQL test reports a clear skipped prerequisite locally while CI keeps Docker enabled.

- [ ] **Step 3: Add command scripts and update CI**

Use these exact command mappings:

```text
scripts/setup            -> ./gradlew dependencies
scripts/dev              -> ./gradlew bootRun
scripts/test-unit        -> ./gradlew unitTest
scripts/test-integration -> ./gradlew integrationTest
scripts/lint             -> ./gradlew check
scripts/typecheck        -> ./gradlew compileJava compileTestJava
scripts/verify           -> ./gradlew clean check integrationTest
```

CI remains Java 26 and invokes only `./scripts/verify` after checkout.

- [ ] **Step 4: Verify a clean backend checkout**

Run: `./scripts/setup && ./scripts/verify`

Expected: PASS with documentation, architecture, OpenAPI, unit, context, and PostgreSQL migration checks visible in Gradle output.

- [ ] **Step 5: Update the quality score and commit**

```bash
git add build.gradle scripts src/test docs/development .github/workflows docs/QUALITY_SCORE.md
git commit -m "chore(harness): unify backend verification commands"
```

### Task 5: Backend final review

**Files:**
- Review only: all backend branch changes.

- [ ] **Step 1: Inspect scope and sensitive data**

Run: `git status --short && git diff origin/main...HEAD --check && git diff origin/main...HEAD --stat`

Expected: only harness, documentation, test, and generated OpenAPI contract files; no `.env` or credential values.

- [ ] **Step 2: Run the authoritative verification**

Run: `./scripts/verify`

Expected: PASS.

- [ ] **Step 3: Confirm the branch history**

Run: `git log --oneline origin/main..HEAD`

Expected: atomic `chore` commits including the already committed design and plan.
