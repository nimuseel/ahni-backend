# Grade History Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Do not dispatch subagents and do not create a worktree.

**Goal:** Add authenticated manual grade registration and personal grade-history listing on top of the student profile and course catalog.

**Architecture:** Add `student_grade` as an attempt aggregate referencing `student` and `course`, with immutable credit and grade-point snapshots. `GradeService` derives ownership from the Supabase JWT subject, resolves only active courses, translates duplicate races, and maps entities to dedicated DTOs; `GradeController` exposes authenticated `POST` and `GET /api/v1/grades` endpoints.

**Tech Stack:** Java 26, Spring Boot 4.1.1, Spring Data JPA, PostgreSQL 18, Flyway, springdoc OpenAPI 3.1.0, JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers 1.21.4

**Spec:** `docs/superpowers/specs/2026-09-15-grade-history-design.md`

## Global Constraints

- Work on `feat/grade-history` in the existing checkout; do not create a worktree.
- Do not use subagents or the removed `ponytail` skill.
- Use Controller -> Service -> Repository -> Entity and never expose JPA entities.
- Derive student ownership exclusively from the JWT subject; request DTOs never accept a student identifier.
- Resolve an active `course` by external UUID before registration.
- Store credit as `BigDecimal`/`numeric(4,1)` and grade point as `BigDecimal`/`numeric(3,2)`.
- Preserve credit and grade point as attempt snapshots; clients never submit grade point.
- Persist enums by string name, never ordinal.
- Keep GPA, OCR, grade mutation, automatic retake behavior, and administrator access out of scope.
- Every HTTP change includes controller tests, OpenAPI success/error examples, bearer security, and the checked-in contract.
- Use TDD and commit each independently testable task.
- Use only `feat`, `fix`, `docs`, `refactor`, or `chore` commit prefixes.
- Run `./scripts/verify` before the final push.

## File Map

### Create

- `src/main/resources/db/migration/V12__create_student_grade.sql`: grade-attempt schema and database constraints.
- `src/main/java/com/ahni/backend/domain/AcademicTerm.java`: term values and chronological sequence.
- `src/main/java/com/ahni/backend/domain/GradeCode.java`: supported codes and phase-one grade-point conversion.
- `src/main/java/com/ahni/backend/entity/StudentGrade.java`: attempt identity, snapshots, and invariants.
- `src/main/java/com/ahni/backend/repository/StudentGradeRepository.java`: duplicate and owned-list queries.
- `src/main/java/com/ahni/backend/dto/GradeRegistrationRequest.java`: manual-entry HTTP request.
- `src/main/java/com/ahni/backend/dto/GradeCourseResponse.java`: course snapshot view without competing current credit.
- `src/main/java/com/ahni/backend/dto/GradeResponse.java`: grade-history HTTP response.
- `src/main/java/com/ahni/backend/exception/CourseNotFoundException.java`: inactive-or-missing course error.
- `src/main/java/com/ahni/backend/exception/GradeAlreadyRegisteredException.java`: duplicate-attempt conflict.
- `src/main/java/com/ahni/backend/exception/InvalidGradeException.java`: cross-field grade invariant error.
- `src/main/java/com/ahni/backend/service/GradeService.java`: authenticated registration and listing orchestration.
- `src/main/java/com/ahni/backend/controller/GradeController.java`: authenticated grade endpoints and OpenAPI annotations.
- `src/test/java/com/ahni/backend/entity/StudentGradeTest.java`: grade derivation and entity invariants.
- `src/test/java/com/ahni/backend/repository/StudentGradeRepositoryIntegrationTest.java`: PostgreSQL mapping, queries, and constraints.
- `src/test/java/com/ahni/backend/service/GradeServiceTest.java`: ownership, registration, errors, sorting, and mapping.
- `src/test/java/com/ahni/backend/controller/GradeControllerTest.java`: HTTP, authentication, validation, serialization, and errors.

### Modify

- `src/test/java/com/ahni/backend/persistence/PostgreSqlMigrationIntegrationTest.java`: require V12.
- `src/main/java/com/ahni/backend/repository/CourseRepository.java`: active external-ID lookup.
- `src/main/java/com/ahni/backend/exception/GlobalExceptionHandler.java`: stable grade error mappings.
- `src/test/java/com/ahni/backend/config/OpenApiContractTest.java`: nullable grade contract assertions.
- `docs/api/openapi.json`: generated OpenAPI contract.
- `docs/domain/index.md`: grade-attempt and snapshot terminology.
- `docs/product/traceability.md`: UC-S03-to-implementation mapping.

---

### Task 1: Create the PostgreSQL Grade Schema

**Files:**
- Create: `src/main/resources/db/migration/V12__create_student_grade.sql`
- Modify: `src/test/java/com/ahni/backend/persistence/PostgreSqlMigrationIntegrationTest.java`

**Interfaces:**
- Consumes: `student(id)` and `course(id)` from existing migrations.
- Produces: constrained `student_grade` storage used by the JPA entity and repository tests.

- [x] **Step 1: Make the migration test require V12**

Add this literal assertion to `appliesAllMigrationsSuccessfully()`:

```java
() -> assertEquals(MigrationState.SUCCESS, summary.versionedStates().get("12"))
```

- [x] **Step 2: Run the focused migration test and verify RED**

```bash
./gradlew integrationTest --tests '*PostgreSqlMigrationIntegrationTest.appliesAllMigrationsSuccessfully'
```

Expected: FAIL because migration version 12 is absent.

- [x] **Step 3: Add V12 with database constraints**

```sql
CREATE TABLE public.student_grade (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    entity_id uuid NOT NULL DEFAULT gen_random_uuid(),
    student_id bigint NOT NULL,
    course_id bigint NOT NULL,
    academic_year smallint NOT NULL,
    term varchar(20) NOT NULL,
    grade_code varchar(20),
    grade_point numeric(3,2),
    credit numeric(4,1) NOT NULL,
    is_rpl boolean NOT NULL DEFAULT false,
    is_retake boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uk_student_grade_entity_id UNIQUE (entity_id),
    CONSTRAINT uk_student_grade_attempt
        UNIQUE (student_id, course_id, academic_year, term),
    CONSTRAINT fk_student_grade_student
        FOREIGN KEY (student_id) REFERENCES public.student (id),
    CONSTRAINT fk_student_grade_course
        FOREIGN KEY (course_id) REFERENCES public.course (id),
    CONSTRAINT ck_student_grade_academic_year
        CHECK (academic_year BETWEEN 2000 AND 9999),
    CONSTRAINT ck_student_grade_term
        CHECK (term IN ('FIRST', 'SUMMER', 'SECOND', 'WINTER')),
    CONSTRAINT ck_student_grade_code
        CHECK (
            grade_code IS NULL OR grade_code IN (
                'A_PLUS', 'A_ZERO', 'B_PLUS', 'B_ZERO',
                'C_PLUS', 'C_ZERO', 'D_PLUS', 'D_ZERO',
                'F', 'P', 'NP'
            )
        ),
    CONSTRAINT ck_student_grade_point
        CHECK (grade_point IS NULL OR grade_point BETWEEN 0.00 AND 4.50),
    CONSTRAINT ck_student_grade_credit
        CHECK (credit > 0.0 AND credit <= 30.0),
    CONSTRAINT ck_student_grade_state
        CHECK (
            (is_rpl AND grade_code IS NULL AND grade_point IS NULL)
            OR
            (
                NOT is_rpl
                AND grade_code IN ('P', 'NP')
                AND grade_point IS NULL
            )
            OR
            (
                NOT is_rpl
                AND grade_code IN (
                    'A_PLUS', 'A_ZERO', 'B_PLUS', 'B_ZERO',
                    'C_PLUS', 'C_ZERO', 'D_PLUS', 'D_ZERO', 'F'
                )
                AND grade_point IS NOT NULL
            )
        )
);

CREATE INDEX idx_student_grade_student
    ON public.student_grade (student_id);

CREATE INDEX idx_student_grade_course
    ON public.student_grade (course_id);
```

- [x] **Step 4: Verify GREEN**

Run the focused migration test again. Expected: V1-V12 all report `SUCCESS` on PostgreSQL 18.

- [x] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V12__create_student_grade.sql \
  src/test/java/com/ahni/backend/persistence/PostgreSqlMigrationIntegrationTest.java
git commit -m "feat(grade): 성적 이력 스키마 추가"
```

### Task 2: Add Grade Domain Invariants

**Files:**
- Create: `src/main/java/com/ahni/backend/domain/AcademicTerm.java`
- Create: `src/main/java/com/ahni/backend/domain/GradeCode.java`
- Create: `src/main/java/com/ahni/backend/entity/StudentGrade.java`
- Create: `src/test/java/com/ahni/backend/entity/StudentGradeTest.java`

**Interfaces:**
- Produces: `StudentGrade(Student, Course, int, AcademicTerm, GradeCode, BigDecimal, boolean, boolean)` and getters consumed by persistence and service mapping.
- Produces: `AcademicTerm.sequence()` and nullable `GradeCode.gradePoint()`.

- [x] **Step 1: Write failing entity tests**

Use literal, independently calculated expectations. Include parameterized conversion coverage:

```java
@ParameterizedTest
@MethodSource("letterGradeCases")
void 등급으로_평점_스냅샷을_생성한다(GradeCode code, String point) {
    StudentGrade grade = new StudentGrade(
        student,
        course,
        2025,
        AcademicTerm.SECOND,
        code,
        new BigDecimal("3.0"),
        false,
        false
    );

    assertThat(grade.getGradePoint()).isEqualByComparingTo(point);
}

static Stream<Arguments> letterGradeCases() {
    return Stream.of(
        arguments(GradeCode.A_PLUS, "4.50"),
        arguments(GradeCode.A_ZERO, "4.00"),
        arguments(GradeCode.B_PLUS, "3.50"),
        arguments(GradeCode.B_ZERO, "3.00"),
        arguments(GradeCode.C_PLUS, "2.50"),
        arguments(GradeCode.C_ZERO, "2.00"),
        arguments(GradeCode.D_PLUS, "1.50"),
        arguments(GradeCode.D_ZERO, "1.00"),
        arguments(GradeCode.F, "0.00")
    );
}
```

Add separate tests proving:

- `P` and `NP` store a null grade point;
- RPL requires a null grade code and stores a null grade point;
- non-RPL rejects a null grade code;
- student, course, and term are required;
- year `2000` and the current Korean year are accepted; `1999` and next year are rejected;
- credit `0.1` and `30.0` are accepted; null, `0.0`, `30.1`, and `3.25` are rejected;
- retake status is retained without modifying another record.

- [x] **Step 2: Verify RED**

```bash
./gradlew test --tests '*StudentGradeTest'
```

Expected: compilation fails because `AcademicTerm`, `GradeCode`, and `StudentGrade` do not exist.

- [x] **Step 3: Implement enums and entity**

```java
public enum AcademicTerm {
    FIRST(1), SUMMER(2), SECOND(3), WINTER(4);

    private final int sequence;

    AcademicTerm(int sequence) {
        this.sequence = sequence;
    }

    public int sequence() {
        return sequence;
    }
}
```

```java
public enum GradeCode {
    A_PLUS("4.50"), A_ZERO("4.00"),
    B_PLUS("3.50"), B_ZERO("3.00"),
    C_PLUS("2.50"), C_ZERO("2.00"),
    D_PLUS("1.50"), D_ZERO("1.00"),
    F("0.00"), P(null), NP(null);

    private final BigDecimal gradePoint;

    GradeCode(String gradePoint) {
        this.gradePoint = gradePoint == null ? null : new BigDecimal(gradePoint);
    }

    public BigDecimal gradePoint() {
        return gradePoint;
    }
}
```

Map `StudentGrade` to `student_grade` with lazy non-null `Student` and `Course` associations, string enums, exact numeric columns, booleans mapped to `is_rpl`/`is_retake`, UUID external identity, and `Instant` lifecycle callbacks. Validate before assigning fields:

```java
if (student == null || course == null || term == null) {
    throw new IllegalArgumentException("학생, 과목, 학기는 필수입니다.");
}
if (academicYear < 2000 || academicYear > Year.now(ZoneId.of("Asia/Seoul")).getValue()) {
    throw new IllegalArgumentException("수강연도가 올바르지 않습니다.");
}
if (credit == null || credit.compareTo(new BigDecimal("0.0")) <= 0
    || credit.compareTo(new BigDecimal("30.0")) > 0
    || credit.stripTrailingZeros().scale() > 1) {
    throw new IllegalArgumentException("학점은 0보다 크고 30 이하여야 하며 소수점 한 자리까지만 입력할 수 있습니다.");
}
if (rpl && gradeCode != null) {
    throw new IllegalArgumentException("RPL 성적에는 등급을 입력할 수 없습니다.");
}
if (!rpl && gradeCode == null) {
    throw new IllegalArgumentException("일반 성적에는 등급이 필수입니다.");
}
this.gradePoint = rpl ? null : gradeCode.gradePoint();
```

- [x] **Step 4: Verify GREEN**

```bash
./gradlew test --tests '*StudentGradeTest'
```

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/ahni/backend/domain/AcademicTerm.java \
  src/main/java/com/ahni/backend/domain/GradeCode.java \
  src/main/java/com/ahni/backend/entity/StudentGrade.java \
  src/test/java/com/ahni/backend/entity/StudentGradeTest.java
git commit -m "feat(grade): 성적 이력 도메인 모델 추가"
```

### Task 3: Persist and Load Student Grade Attempts

**Files:**
- Create: `src/main/java/com/ahni/backend/repository/StudentGradeRepository.java`
- Modify: `src/main/java/com/ahni/backend/repository/CourseRepository.java`
- Create: `src/test/java/com/ahni/backend/repository/StudentGradeRepositoryIntegrationTest.java`

**Interfaces:**
- Produces duplicate detection, current-student list loading with eager course data, and active course lookup.

- [ ] **Step 1: Write failing PostgreSQL repository tests**

Persist real `Student`, `Department`, `Course`, and `StudentGrade` rows. Prove these behaviors with separate tests:

```java
assertThat(repository.existsByStudentAndCourseAndAcademicYearAndTerm(
    student,
    course,
    2025,
    AcademicTerm.SECOND
)).isTrue();
```

```java
List<StudentGrade> found = repository.findAllByStudent(firstStudent);

assertThat(found)
    .extracting(grade -> grade.getStudent().getAuthUserId())
    .containsOnly(firstStudent.getAuthUserId());
assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(found.getFirst().getCourse()))
    .isTrue();
assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(
    found.getFirst().getCourse().getDepartment()
)).isTrue();
```

Also prove:

- saved UUID and timestamps are populated;
- the same course can be stored in another term or for another student;
- the same student/course/year/term tuple is rejected;
- `findByEntityIdAndActiveTrue` excludes a row changed to `is_active = false` through `JdbcTemplate`;
- raw SQL rejects invalid year, term, grade code, grade-point range, credit, and invalid RPL/grade state.

Cleanup order in `@BeforeEach` is `student_grade`, `course`, `student_major`, `student`, then `department` to respect foreign keys.

- [ ] **Step 2: Verify RED**

```bash
./gradlew integrationTest --tests '*StudentGradeRepositoryIntegrationTest'
```

Expected: compilation fails because `StudentGradeRepository` and the active external-ID method are absent.

- [ ] **Step 3: Add repository interfaces**

```java
public interface StudentGradeRepository extends JpaRepository<StudentGrade, Long> {
    boolean existsByStudentAndCourseAndAcademicYearAndTerm(
        Student student,
        Course course,
        int academicYear,
        AcademicTerm term
    );

    @EntityGraph(attributePaths = {"course", "course.department"})
    List<StudentGrade> findAllByStudent(Student student);
}
```

Add to `CourseRepository`:

```java
Optional<Course> findByEntityIdAndActiveTrue(UUID entityId);
```

- [ ] **Step 4: Verify GREEN against PostgreSQL 18**

```bash
./gradlew integrationTest --tests '*StudentGradeRepositoryIntegrationTest'
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ahni/backend/repository/StudentGradeRepository.java \
  src/main/java/com/ahni/backend/repository/CourseRepository.java \
  src/test/java/com/ahni/backend/repository/StudentGradeRepositoryIntegrationTest.java
git commit -m "feat(grade): 학생 성적 저장소 추가"
```

### Task 4: Add Authenticated Grade Service

**Files:**
- Create: `src/main/java/com/ahni/backend/dto/GradeRegistrationRequest.java`
- Create: `src/main/java/com/ahni/backend/dto/GradeCourseResponse.java`
- Create: `src/main/java/com/ahni/backend/dto/GradeResponse.java`
- Create: `src/main/java/com/ahni/backend/exception/CourseNotFoundException.java`
- Create: `src/main/java/com/ahni/backend/exception/GradeAlreadyRegisteredException.java`
- Create: `src/main/java/com/ahni/backend/exception/InvalidGradeException.java`
- Create: `src/main/java/com/ahni/backend/service/GradeService.java`
- Create: `src/test/java/com/ahni/backend/service/GradeServiceTest.java`

**Interfaces:**
- Produces: `GradeResponse register(UUID authUserId, GradeRegistrationRequest request)`.
- Produces: `List<GradeResponse> getGrades(UUID authUserId)`.

- [ ] **Step 1: Write failing service tests**

Use complete entity fixtures and assert returned values, not only Mockito interactions. Cover:

```java
GradeResponse result = gradeService.register(authUserId, new GradeRegistrationRequest(
    course.getEntityId(),
    2025,
    AcademicTerm.SECOND,
    GradeCode.A_PLUS,
    new BigDecimal("3.0"),
    false,
    false
));

assertThat(result.gradePoint()).isEqualByComparingTo("4.50");
assertThat(result.credit()).isEqualByComparingTo("3.0");
assertThat(result.course().code()).isEqualTo("CSE101");
```

Add separate tests for:

- RPL maps nullable grade code and point;
- missing student stops before course lookup;
- missing or inactive course returns `CourseNotFoundException`;
- an existing tuple returns `GradeAlreadyRegisteredException` without saving;
- entity validation errors become `InvalidGradeException`;
- a `DataIntegrityViolationException` from `saveAndFlush` becomes `GradeAlreadyRegisteredException`;
- listing resolves only the authenticated student;
- listing maps nullable course department;
- listing sorts year descending, term sequence descending, then code ascending;
- no records returns an empty list.

- [ ] **Step 2: Verify RED**

```bash
./gradlew test --tests '*GradeServiceTest'
```

Expected: compilation fails because the service, DTOs, and exceptions do not exist.

- [ ] **Step 3: Add DTOs and exceptions**

```java
public record GradeRegistrationRequest(
    @NotNull UUID courseEntityId,
    @Min(2000) int academicYear,
    @NotNull AcademicTerm term,
    GradeCode gradeCode,
    @NotNull @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax("30.0") @Digits(integer = 2, fraction = 1) BigDecimal credit,
    @Schema(defaultValue = "false") boolean rpl,
    @Schema(defaultValue = "false") boolean retake
) { }
```

```java
public record GradeCourseResponse(
    UUID entityId,
    String code,
    String name,
    CourseCategory category,
    @Schema(nullable = true) DepartmentResponse department
) { }
```

```java
public record GradeResponse(
    UUID entityId,
    GradeCourseResponse course,
    int academicYear,
    AcademicTerm term,
    @Schema(nullable = true) GradeCode gradeCode,
    @Schema(nullable = true) BigDecimal gradePoint,
    BigDecimal credit,
    boolean rpl,
    boolean retake,
    Instant createdAt,
    Instant updatedAt
) { }
```

Use these stable exception messages:

```java
new CourseNotFoundException()              // "과목을 찾을 수 없습니다."
new GradeAlreadyRegisteredException()      // "해당 학기의 과목 성적이 이미 등록되어 있습니다."
new InvalidGradeException(cause.getMessage())
```

- [ ] **Step 4: Implement minimal service behavior**

Registration selects the student and course, checks the tuple, constructs the entity, and flushes inside a persistence-conflict translation boundary:

```java
public GradeResponse register(UUID authUserId, GradeRegistrationRequest request) {
    Student student = studentRepository.findByAuthUserId(authUserId)
        .orElseThrow(StudentNotFoundException::new);
    Course course = courseRepository.findByEntityIdAndActiveTrue(request.courseEntityId())
        .orElseThrow(CourseNotFoundException::new);

    if (gradeRepository.existsByStudentAndCourseAndAcademicYearAndTerm(
        student, course, request.academicYear(), request.term()
    )) {
        throw new GradeAlreadyRegisteredException();
    }

    StudentGrade grade;
    try {
        grade = new StudentGrade(
            student, course, request.academicYear(), request.term(),
            request.gradeCode(), request.credit(), request.rpl(), request.retake()
        );
    } catch (IllegalArgumentException exception) {
        throw new InvalidGradeException(exception.getMessage());
    }

    try {
        return toResponse(gradeRepository.saveAndFlush(grade));
    } catch (DataIntegrityViolationException exception) {
        throw new GradeAlreadyRegisteredException();
    }
}
```

Listing uses this literal comparator:

```java
private static final Comparator<StudentGrade> NEWEST_FIRST =
    Comparator.comparingInt(StudentGrade::getAcademicYear).reversed()
        .thenComparing(
            grade -> grade.getTerm().sequence(),
            Comparator.reverseOrder()
        )
        .thenComparing(grade -> grade.getCourse().getCode());
```

- [ ] **Step 5: Verify GREEN**

```bash
./gradlew test --tests '*GradeServiceTest' --tests '*StudentServiceTest' --tests '*CourseServiceTest'
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/ahni/backend/dto/GradeRegistrationRequest.java \
  src/main/java/com/ahni/backend/dto/GradeCourseResponse.java \
  src/main/java/com/ahni/backend/dto/GradeResponse.java \
  src/main/java/com/ahni/backend/exception/CourseNotFoundException.java \
  src/main/java/com/ahni/backend/exception/GradeAlreadyRegisteredException.java \
  src/main/java/com/ahni/backend/exception/InvalidGradeException.java \
  src/main/java/com/ahni/backend/service/GradeService.java \
  src/test/java/com/ahni/backend/service/GradeServiceTest.java
git commit -m "feat(grade): 성적 등록 및 조회 서비스 추가"
```

### Task 5: Expose Authenticated Grade APIs

**Files:**
- Create: `src/main/java/com/ahni/backend/controller/GradeController.java`
- Modify: `src/main/java/com/ahni/backend/exception/GlobalExceptionHandler.java`
- Create: `src/test/java/com/ahni/backend/controller/GradeControllerTest.java`

**Interfaces:**
- Produces: authenticated `POST /api/v1/grades` and `GET /api/v1/grades`.
- Returns: `GradeResponse`, `List<GradeResponse>`, or the existing `ApiErrorResponse`.

- [ ] **Step 1: Write failing MockMvc tests**

The success test must use a literal JWT subject and complete response fixture:

```java
mockMvc.perform(post("/api/v1/grades")
        .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString())))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "courseEntityId": "00000000-0000-0000-0000-000000000101",
              "academicYear": 2025,
              "term": "SECOND",
              "gradeCode": "A_PLUS",
              "credit": 3.0,
              "rpl": false,
              "retake": false
            }
            """))
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.course.code").value("CSE101"))
    .andExpect(jsonPath("$.gradePoint").value(4.50));
```

Add separate tests for:

- authenticated list serialization and deterministic service result;
- empty list returns `[]`;
- RPL response emits null `gradeCode` and `gradePoint`;
- missing JWT returns `401` for both methods without service interaction;
- malformed JSON, UUID, enum, missing course/term/credit, and invalid decimal credit return `400 INVALID_REQUEST`;
- `InvalidGradeException` returns `400 INVALID_GRADE`;
- missing student and course return their stable `404` codes;
- duplicate grade returns `409 GRADE_ALREADY_REGISTERED`.

- [ ] **Step 2: Verify RED**

```bash
./gradlew integrationTest --tests '*GradeControllerTest'
```

Expected: compilation fails because `GradeController` does not exist.

- [ ] **Step 3: Add stable error mappings**

Add handlers to `GlobalExceptionHandler`:

```java
@ExceptionHandler(InvalidGradeException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ApiErrorResponse handleInvalidGrade(InvalidGradeException exception) {
    return new ApiErrorResponse("INVALID_GRADE", exception.getMessage());
}

@ExceptionHandler(CourseNotFoundException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
public ApiErrorResponse handleCourseNotFound(CourseNotFoundException exception) {
    return new ApiErrorResponse("COURSE_NOT_FOUND", exception.getMessage());
}

@ExceptionHandler(GradeAlreadyRegisteredException.class)
@ResponseStatus(HttpStatus.CONFLICT)
public ApiErrorResponse handleGradeAlreadyRegistered(GradeAlreadyRegisteredException exception) {
    return new ApiErrorResponse("GRADE_ALREADY_REGISTERED", exception.getMessage());
}
```

- [ ] **Step 4: Add the controller and OpenAPI annotations**

```java
@RestController
@RequestMapping("/api/v1/grades")
public class GradeController {
    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GradeResponse register(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody GradeRegistrationRequest request
    ) {
        return gradeService.register(UUID.fromString(jwt.getSubject()), request);
    }

    @GetMapping
    public List<GradeResponse> getGrades(@AuthenticationPrincipal Jwt jwt) {
        return gradeService.getGrades(UUID.fromString(jwt.getSubject()));
    }
}
```

Annotate both operations with `@Operation(security = @SecurityRequirement(name = "bearerAuth"))`. Document complete request and response examples. POST documents `201`, `400`, `401`, `404`, and `409`; GET documents `200`, `401`, and `404`. Use `ApiErrorResponse` schemas and stable literal examples for every application error code.

- [ ] **Step 5: Verify GREEN**

```bash
./gradlew integrationTest \
  --tests '*GradeControllerTest' \
  --tests '*SecurityConfigurationTest' \
  --tests '*LayerDependencyTest'
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/ahni/backend/controller/GradeController.java \
  src/main/java/com/ahni/backend/exception/GlobalExceptionHandler.java \
  src/test/java/com/ahni/backend/controller/GradeControllerTest.java
git commit -m "feat(grade): 성적 등록 및 조회 API 추가"
```

### Task 6: Refresh Contract and Domain Documentation

**Files:**
- Modify: `src/test/java/com/ahni/backend/config/OpenApiContractTest.java`
- Modify: `docs/api/openapi.json`
- Modify: `docs/domain/index.md`
- Modify: `docs/product/traceability.md`

**Interfaces:**
- Produces runtime and checked-in OpenAPI semantic equality plus durable grade-history rules.

- [ ] **Step 1: Add focused nullable-contract assertions**

Add a test that fetches `/v3/api-docs` and asserts the object reference uses `type: null` and nullable scalar types include `null`:

```java
var schemas = objectMapper.readTree(actual).at("/components/schemas");
assertEquals(
    "null",
    schemas.at("/GradeCourseResponse/properties/department/type").asText()
);
assertEquals(
    objectMapper.readTree("""["string", "null"]"""),
    schemas.at("/GradeResponse/properties/gradeCode/type")
);
assertEquals(
    objectMapper.readTree("""["number", "null"]"""),
    schemas.at("/GradeResponse/properties/gradePoint/type")
);
```

Run the focused test and ensure it passes against the controller annotations before refreshing the checked-in file.

- [ ] **Step 2: Run the checked-in contract test and verify RED**

```bash
./gradlew integrationTest --tests '*OpenApiContractTest.generatedContractMatchesCheckedInContract'
```

Expected: FAIL and generate `build/openapi/openapi.json` because the grade paths and schemas are absent from the checked-in contract.

- [ ] **Step 3: Review and copy the generated contract**

Inspect with:

```bash
jq '{
  gradePath: .paths["/api/v1/grades"],
  request: .components.schemas.GradeRegistrationRequest,
  response: .components.schemas.GradeResponse
}' build/openapi/openapi.json
```

Confirm bearer security, request required fields/default booleans, nullable RPL fields, complete success examples, and all documented error responses. Then copy the generated artifact:

```bash
cp build/openapi/openapi.json docs/api/openapi.json
```

- [ ] **Step 4: Update durable domain and traceability docs**

Add to `docs/domain/index.md`:

```markdown
- `student_grade`: one student course attempt identified by academic year and term. Credit and grade point are immutable attempt snapshots; RPL has no grade code or grade point and is excluded from future GPA calculations.
```

Add a UC-S03 row to `docs/product/traceability.md` naming V12, entity, repository, service, controller, and OpenAPI tests. State that GPA, OCR, and retake calculation remain planned consumers rather than behavior in this slice.

- [ ] **Step 5: Verify the complete harness**

```bash
./scripts/verify
```

Expected: unit tests, PostgreSQL 18 migration/repository integration tests, MVC/security tests, architecture rules, and OpenAPI contract all pass.

- [ ] **Step 6: Commit and push**

```bash
git add src/test/java/com/ahni/backend/config/OpenApiContractTest.java \
  docs/api/openapi.json docs/domain/index.md docs/product/traceability.md \
  docs/superpowers/plans/2026-09-15-grade-history.md
git commit -m "docs(grade): 성적 이력 계약 문서화"
git push -u origin feat/grade-history
```
