# Student Major Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Do not dispatch subagents and do not create a worktree.

**Goal:** Extend student registration, profile lookup, and profile correction to support exactly one primary major, at most one double major, and at most one minor.

**Architecture:** Keep the normalized `student_major` relation and make PostgreSQL the final cardinality guard. `StudentService` validates the complete configuration, resolves active departments, and owns transactional replacement; `StudentController` exposes backward-compatible registration/read contracts and one complete-replacement endpoint. Mobile work is intentionally deferred to a separate plan after this backend contract is merged.

**Tech Stack:** Java 26, Spring Boot 4.1.1, Spring Data JPA, PostgreSQL 18, Flyway, springdoc OpenAPI 3.1.0, JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers 1.21.4

**Spec:** `docs/superpowers/specs/2026-09-14-student-major-management-design.md`

## Global Constraints

- Start `feat/student-major-management` from the latest `origin/main` after the design PR is merged.
- Work in the existing checkout; do not create a worktree.
- Keep `MajorType` limited to `PRIMARY`, `DOUBLE_MAJOR`, and `MINOR`; do not add `MULTIPLE_MAJOR`.
- A student has exactly one active primary major, at most one active double major, and at most one active minor.
- The same active department cannot be assigned to more than one major type for a student.
- Preserve existing registration clients that send only `primaryDepartmentEntityId`.
- Derive student ownership from the authenticated JWT subject; never accept a student identifier in these requests.
- Every HTTP change includes controller tests, OpenAPI annotations/examples, stable errors, and an updated checked-in OpenAPI contract.
- Use TDD for each behavior and commit only after its focused tests pass.
- Use `feat`, `fix`, `docs`, `refactor`, or `chore` Conventional Commit prefixes according to the change.
- Run `./scripts/verify` before the final push.

## File Map

### Create

- `src/main/resources/db/migration/V10__enforce_student_major_cardinality.sql`: active type and department uniqueness.
- `src/main/java/com/ahni/backend/dto/StudentMajorUpdateRequest.java`: complete major-replacement request.
- `src/main/java/com/ahni/backend/exception/DuplicateMajorDepartmentException.java`: stable duplicate-department domain error.

### Modify

- `src/main/java/com/ahni/backend/dto/StudentProfileRegistrationRequest.java`: optional double-major and minor identifiers.
- `src/main/java/com/ahni/backend/dto/StudentProfileResponse.java`: nullable double-major and minor department objects.
- `src/main/java/com/ahni/backend/entity/StudentMajor.java`: soft-delete behavior required by replacement.
- `src/main/java/com/ahni/backend/repository/StudentMajorRepository.java`: load all active assignments for a student.
- `src/main/java/com/ahni/backend/service/StudentService.java`: validate, resolve, register, read, and replace complete configurations.
- `src/main/java/com/ahni/backend/controller/StudentController.java`: extended examples and `PUT /api/v1/students/me/majors`.
- `src/main/java/com/ahni/backend/exception/GlobalExceptionHandler.java`: `DUPLICATE_MAJOR_DEPARTMENT` mapping.
- `src/test/java/com/ahni/backend/persistence/PostgreSqlMigrationIntegrationTest.java`: require Flyway V10.
- `src/test/java/com/ahni/backend/entity/StudentMajorTest.java`: soft-delete behavior.
- `src/test/java/com/ahni/backend/repository/StudentRepositoryIntegrationTest.java`: PostgreSQL constraints and active-major lookup.
- `src/test/java/com/ahni/backend/service/StudentServiceTest.java`: registration, lookup, validation, and replacement rules.
- `src/test/java/com/ahni/backend/controller/StudentControllerTest.java`: serialized contracts, auth, and errors.
- `docs/api/openapi.json`: generated runtime API contract.
- `docs/domain/index.md`: approved cardinalities and terminology.

---

### Task 1: Enforce Major Cardinality in PostgreSQL

**Files:**
- Create: `src/main/resources/db/migration/V10__enforce_student_major_cardinality.sql`
- Modify: `src/test/java/com/ahni/backend/persistence/PostgreSqlMigrationIntegrationTest.java`
- Modify: `src/test/java/com/ahni/backend/repository/StudentRepositoryIntegrationTest.java`

**Interfaces:**
- Consumes: `student_major(student_id, department_id, major_type, deleted_at)` from V7.
- Produces: active uniqueness by `(student_id, major_type)` and `(student_id, department_id)` for all later service operations.

- [ ] **Step 1: Make the migration summary test require V10**

Replace the final V9 assertion with this comma-separated V9/V10 pair in `appliesAllMigrationsSuccessfully()`:

```java
() -> assertEquals(MigrationState.SUCCESS, summary.versionedStates().get("9")),
() -> assertEquals(MigrationState.SUCCESS, summary.versionedStates().get("10"))
```

- [ ] **Step 2: Run the migration test and verify it fails**

Run:

```bash
./gradlew integrationTest --tests '*PostgreSqlMigrationIntegrationTest.appliesAllMigrationsSuccessfully'
```

Expected: FAIL because migration version `10` is absent.

- [ ] **Step 3: Add the V10 migration**

Create `V10__enforce_student_major_cardinality.sql` with exactly these index changes:

```sql
DROP INDEX public.uk_student_major_active_assignment;
DROP INDEX public.uk_student_major_active_primary;

CREATE UNIQUE INDEX uk_student_major_active_type
    ON public.student_major (student_id, major_type)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_student_major_active_department
    ON public.student_major (student_id, department_id)
    WHERE deleted_at IS NULL;
```

Do not delete or rewrite conflicting production rows in this migration. Existing invalid data must block deployment so it can be inspected instead of being silently discarded.

- [ ] **Step 4: Verify V1-V10 migrate successfully**

Run:

```bash
./gradlew integrationTest --tests '*PostgreSqlMigrationIntegrationTest.appliesAllMigrationsSuccessfully'
```

Expected: PASS with versions 1 through 10 in `SUCCESS` state.

- [ ] **Step 5: Add failing repository tests for the two constraints**

Add these tests to `StudentRepositoryIntegrationTest`. Reuse the class's repositories and PostgreSQL container.

```java
@Test
void 학생은_같은_유형의_활성_전공을_두_개_가질_수_없다() {
    Student student = studentRepository.saveAndFlush(new Student(
        UUID.randomUUID(),
        "major-type@inha.edu",
        2024,
        EnrollmentStatus.ENROLLED,
        "인하"
    ));
    Department first = departmentRepository.saveAndFlush(new Department("금융투자학과"));
    Department second = departmentRepository.saveAndFlush(new Department("산업경영학과"));

    studentMajorRepository.saveAndFlush(new StudentMajor(student, first, MajorType.DOUBLE_MAJOR));

    assertThatThrownBy(() -> studentMajorRepository.saveAndFlush(
        new StudentMajor(student, second, MajorType.DOUBLE_MAJOR)
    )).isInstanceOf(DataIntegrityViolationException.class);
}

@Test
void 학생은_같은_학과를_다른_전공_유형으로_중복할_수_없다() {
    Student student = studentRepository.saveAndFlush(new Student(
        UUID.randomUUID(),
        "major-department@inha.edu",
        2024,
        EnrollmentStatus.ENROLLED,
        "인하"
    ));
    Department department = departmentRepository.saveAndFlush(new Department("반도체산업융합학과"));

    studentMajorRepository.saveAndFlush(new StudentMajor(student, department, MajorType.PRIMARY));

    assertThatThrownBy(() -> studentMajorRepository.saveAndFlush(
        new StudentMajor(student, department, MajorType.MINOR)
    )).isInstanceOf(DataIntegrityViolationException.class);
}
```

- [ ] **Step 6: Run the repository constraint tests**

Run:

```bash
./gradlew integrationTest --tests '*StudentRepositoryIntegrationTest*활성_전공*' --tests '*StudentRepositoryIntegrationTest*같은_학과*'
```

Expected: PASS against PostgreSQL 18.

- [ ] **Step 7: Commit the persistence boundary**

```bash
git add src/main/resources/db/migration/V10__enforce_student_major_cardinality.sql \
  src/test/java/com/ahni/backend/persistence/PostgreSqlMigrationIntegrationTest.java \
  src/test/java/com/ahni/backend/repository/StudentRepositoryIntegrationTest.java
git commit -m "feat(major): 전공 구성 제약 추가"
```

### Task 2: Load and Soft-Delete Complete Major Assignments

**Files:**
- Modify: `src/main/java/com/ahni/backend/entity/StudentMajor.java`
- Modify: `src/main/java/com/ahni/backend/repository/StudentMajorRepository.java`
- Modify: `src/test/java/com/ahni/backend/entity/StudentMajorTest.java`
- Modify: `src/test/java/com/ahni/backend/repository/StudentRepositoryIntegrationTest.java`

**Interfaces:**
- Produces: `StudentMajor.softDelete()`, `StudentMajor.isAssignedTo(Department)`, and `findAllByStudentAndDeletedAtIsNull(Student)`.
- Consumers: registration/profile mapping in Task 3 and replacement in Task 4.

- [ ] **Step 1: Write failing entity tests for replacement primitives**

Add these tests to `StudentMajorTest`:

```java
@Test
void 전공_관계를_소프트_삭제할_수_있다() {
    StudentMajor major = new StudentMajor(
        createStudent(),
        new Department("산업경영학과"),
        MajorType.MINOR
    );

    major.softDelete();

    assertThat(major.getDeletedAt()).isNotNull();
}

@Test
void 같은_학과에_연결되어_있는지_확인할_수_있다() {
    Department department = new Department("소프트웨어융합공학과");
    StudentMajor major = new StudentMajor(createStudent(), department, MajorType.PRIMARY);

    assertThat(major.isAssignedTo(department)).isTrue();
}

private Student createStudent() {
    return new Student(
        UUID.randomUUID(),
        "student@inha.edu",
        2024,
        EnrollmentStatus.ENROLLED,
        "인하"
    );
}
```

- [ ] **Step 2: Verify the entity tests fail to compile**

Run:

```bash
./gradlew test --tests '*StudentMajorTest'
```

Expected: FAIL because `softDelete()` and `isAssignedTo(Department)` do not exist.

- [ ] **Step 3: Add the minimal entity behavior**

Add these methods to `StudentMajor`:

```java
public boolean isAssignedTo(Department department) {
    return this.department.getEntityId().equals(department.getEntityId());
}

public void softDelete() {
    if (deletedAt == null) {
        deletedAt = Instant.now();
    }
}
```

Do not add a generic lifecycle abstraction. These two methods are the only behaviors required by replacement.

- [ ] **Step 4: Replace the single-type repository lookup with active-list lookup**

Change `StudentMajorRepository` to:

```java
public interface StudentMajorRepository extends JpaRepository<StudentMajor, Long> {
    List<StudentMajor> findAllByStudentAndDeletedAtIsNull(Student student);
}
```

Import `java.util.List` and remove the unused `MajorType` and `Optional` imports.

Replace the old `findByStudentAndMajorTypeAndDeletedAtIsNull(...)` call in `학생의_활성_주전공을_조회할_수_있다()` with:

```java
List<StudentMajor> found = studentMajorRepository
    .findAllByStudentAndDeletedAtIsNull(student);

assertThat(found)
    .singleElement()
    .satisfies(major -> {
        assertThat(major.getMajorType()).isEqualTo(MajorType.PRIMARY);
        assertThat(major.getDepartment().getEntityId())
            .isEqualTo(department.getEntityId());
    });
```

- [ ] **Step 5: Add the active-list integration test**

Add this test to `StudentRepositoryIntegrationTest`:

```java
@Test
void 학생의_활성_전공만_모두_조회한다() {
    Student student = studentRepository.saveAndFlush(new Student(
        UUID.randomUUID(),
        "active-majors@inha.edu",
        2024,
        EnrollmentStatus.ENROLLED,
        "인하"
    ));
    Department primary = departmentRepository.saveAndFlush(new Department("금융투자학과"));
    Department doubleMajor = departmentRepository.saveAndFlush(new Department("산업경영학과"));
    Department minorDepartment = departmentRepository.saveAndFlush(new Department("메카트로닉스공학과"));

    studentMajorRepository.saveAndFlush(
        new StudentMajor(student, primary, MajorType.PRIMARY)
    );
    studentMajorRepository.saveAndFlush(
        new StudentMajor(student, doubleMajor, MajorType.DOUBLE_MAJOR)
    );
    StudentMajor minor = studentMajorRepository.saveAndFlush(
        new StudentMajor(student, minorDepartment, MajorType.MINOR)
    );
    minor.softDelete();
    studentMajorRepository.flush();
    entityManager.clear();

    List<StudentMajor> activeMajors = studentMajorRepository
        .findAllByStudentAndDeletedAtIsNull(student);

    assertThat(activeMajors)
        .extracting(StudentMajor::getMajorType)
        .containsExactlyInAnyOrder(MajorType.PRIMARY, MajorType.DOUBLE_MAJOR);
}
```

- [ ] **Step 6: Run focused entity and repository tests**

```bash
./gradlew test --tests '*StudentMajorTest'
./gradlew integrationTest --tests '*StudentRepositoryIntegrationTest'
```

Expected: PASS.

- [ ] **Step 7: Commit the entity and repository behavior**

```bash
git add src/main/java/com/ahni/backend/entity/StudentMajor.java \
  src/main/java/com/ahni/backend/repository/StudentMajorRepository.java \
  src/test/java/com/ahni/backend/entity/StudentMajorTest.java \
  src/test/java/com/ahni/backend/repository/StudentRepositoryIntegrationTest.java
git commit -m "feat(major): 활성 전공 관계 조회 지원"
```

### Task 3: Extend Registration and Profile Read Contracts

**Files:**
- Create: `src/main/java/com/ahni/backend/exception/DuplicateMajorDepartmentException.java`
- Modify: `src/main/java/com/ahni/backend/dto/StudentProfileRegistrationRequest.java`
- Modify: `src/main/java/com/ahni/backend/dto/StudentProfileResponse.java`
- Modify: `src/main/java/com/ahni/backend/exception/GlobalExceptionHandler.java`
- Modify: `src/main/java/com/ahni/backend/service/StudentService.java`
- Modify: `src/main/java/com/ahni/backend/controller/StudentController.java`
- Modify: `src/test/java/com/ahni/backend/service/StudentServiceTest.java`
- Modify: `src/test/java/com/ahni/backend/controller/StudentControllerTest.java`

**Interfaces:**
- Extends `StudentProfileRegistrationRequest` with nullable `doubleMajorDepartmentEntityId` and `minorDepartmentEntityId`.
- Extends `StudentProfileResponse` with nullable `doubleMajorDepartment` and `minorDepartment`.
- Produces `DUPLICATE_MAJOR_DEPARTMENT` with HTTP 400.

- [ ] **Step 1: Update existing test constructors to establish the new DTO shape**

Change registration request construction to this parameter order everywhere in tests:

```java
new StudentProfileRegistrationRequest(
    primaryDepartmentEntityId,
    doubleMajorDepartmentEntityId,
    minorDepartmentEntityId,
    admissionYear,
    enrollmentStatus,
    nickname
)
```

Use `null, null` for existing primary-only test cases. Change response construction to place nullable `doubleMajorDepartment` and `minorDepartment` immediately after `primaryDepartment`.

- [ ] **Step 2: Add failing service tests for all three majors and duplicate departments**

Add a successful registration test with three distinct `Department` objects. Use this request and repository verification after stubbing each `findByEntityId` call and returning the first argument from `studentRepository.save(...)`:

```java
StudentProfileRegistrationRequest request = new StudentProfileRegistrationRequest(
    primary.getEntityId(),
    doubleMajor.getEntityId(),
    minor.getEntityId(),
    2024,
    EnrollmentStatus.ENROLLED,
    "인하"
);

StudentProfileResponse response = studentService.registerProfile(
    authUserId,
    "student@inha.edu",
    request
);

verify(studentMajorRepository).saveAll(argThat(majors -> {
    List<MajorType> types = StreamSupport.stream(majors.spliterator(), false)
        .map(StudentMajor::getMajorType)
        .toList();
    return types.size() == 3
        && types.containsAll(List.of(
            MajorType.PRIMARY,
            MajorType.DOUBLE_MAJOR,
            MajorType.MINOR
        ));
}));
assertThat(response.doubleMajorDepartment().entityId())
    .isEqualTo(doubleMajor.getEntityId());
assertThat(response.minorDepartment().entityId())
    .isEqualTo(minor.getEntityId());
```

Add a duplicate test using the same UUID for primary and minor:

```java
assertThatThrownBy(() -> studentService.registerProfile(authUserId, email, request))
    .isInstanceOf(DuplicateMajorDepartmentException.class);

verifyNoInteractions(departmentRepository, studentMajorRepository);
verify(studentRepository, never()).save(any());
```

- [ ] **Step 3: Verify the service tests fail to compile**

```bash
./gradlew test --tests '*StudentServiceTest'
```

Expected: FAIL because DTO fields, exception, and multi-major registration are absent.

- [ ] **Step 4: Extend the request and response records**

Add these optional fields after `primaryDepartmentEntityId` in `StudentProfileRegistrationRequest`:

```java
@Schema(description = "복수전공 학과 식별자", nullable = true)
UUID doubleMajorDepartmentEntityId,

@Schema(description = "부전공 학과 식별자", nullable = true)
UUID minorDepartmentEntityId,
```

Add these fields after `primaryDepartment` in `StudentProfileResponse`:

```java
@Schema(description = "복수전공 학과", nullable = true)
DepartmentResponse doubleMajorDepartment,

@Schema(description = "부전공 학과", nullable = true)
DepartmentResponse minorDepartment,
```

- [ ] **Step 5: Add the stable duplicate exception**

Create:

```java
package com.ahni.backend.exception;

public class DuplicateMajorDepartmentException extends RuntimeException {
    public DuplicateMajorDepartmentException() {
        super("같은 학과를 여러 전공 유형으로 선택할 수 없습니다.");
    }
}
```

Add this handler to `GlobalExceptionHandler`:

```java
@ExceptionHandler(DuplicateMajorDepartmentException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ApiErrorResponse handleDuplicateMajorDepartment(
    DuplicateMajorDepartmentException exception
) {
    return new ApiErrorResponse("DUPLICATE_MAJOR_DEPARTMENT", exception.getMessage());
}
```

- [ ] **Step 6: Implement complete configuration helpers in `StudentService`**

Add these private methods and use an `EnumMap<MajorType, Department>` as the internal representation:

```java
private EnumMap<MajorType, Department> resolveDepartments(
    UUID primaryId,
    UUID doubleMajorId,
    UUID minorId
) {
    validateDistinctDepartments(primaryId, doubleMajorId, minorId);

    EnumMap<MajorType, Department> departments = new EnumMap<>(MajorType.class);
    departments.put(MajorType.PRIMARY, findActiveDepartment(primaryId));
    if (doubleMajorId != null) {
        departments.put(MajorType.DOUBLE_MAJOR, findActiveDepartment(doubleMajorId));
    }
    if (minorId != null) {
        departments.put(MajorType.MINOR, findActiveDepartment(minorId));
    }
    return departments;
}

private void validateDistinctDepartments(UUID primaryId, UUID doubleMajorId, UUID minorId) {
    List<UUID> ids = Stream.of(primaryId, doubleMajorId, minorId)
        .filter(Objects::nonNull)
        .toList();
    if (ids.stream().distinct().count() != ids.size()) {
        throw new DuplicateMajorDepartmentException();
    }
}

private Department findActiveDepartment(UUID entityId) {
    return departmentRepository.findByEntityId(entityId)
        .filter(department -> department.getDeletedAt() == null)
        .orElseThrow(DepartmentNotFoundException::new);
}
```

Use `studentMajorRepository.saveAll(...)` to persist one `StudentMajor` per map entry during registration. Replace the primary-only `toResponse` method with:

```java
private static StudentProfileResponse toResponse(
    Student student,
    Collection<StudentMajor> majors
) {
    EnumMap<MajorType, DepartmentResponse> departments = new EnumMap<>(MajorType.class);
    majors.forEach(major -> departments.put(
        major.getMajorType(),
        new DepartmentResponse(
            major.getDepartment().getEntityId(),
            major.getDepartment().getName()
        )
    ));

    DepartmentResponse primary = Optional
        .ofNullable(departments.get(MajorType.PRIMARY))
        .orElseThrow(() -> new IllegalStateException("학생의 주전공을 찾을 수 없습니다."));

    return new StudentProfileResponse(
        student.getEntityId(),
        student.getEmail(),
        student.getNickname(),
        primary,
        departments.get(MajorType.DOUBLE_MAJOR),
        departments.get(MajorType.MINOR),
        student.getAdmissionYear(),
        student.getEnrollmentStatus(),
        student.getAccountStatus()
    );
}
```

Change `getProfile` to load `findAllByStudentAndDeletedAtIsNull(student)` once and pass that collection to `toResponse`.

Update `인증된_학생의_프로필을_조회한다()` to stub the new repository method:

```java
when(studentMajorRepository.findAllByStudentAndDeletedAtIsNull(student))
    .thenReturn(List.of(primaryMajor));
```

Remove every remaining call to `findByStudentAndMajorTypeAndDeletedAtIsNull(...)` from production and test sources before compiling.

- [ ] **Step 7: Run the service tests**

```bash
./gradlew test --tests '*StudentServiceTest'
```

Expected: PASS for primary-only registration, all-three registration, complete profile lookup, and duplicate validation.

- [ ] **Step 8: Extend registration and lookup controller tests**

Update the successful POST JSON with the two optional identifiers and assert:

```java
.andExpect(jsonPath("$.doubleMajorDepartment.entityId")
    .value(doubleMajorDepartmentEntityId.toString()))
.andExpect(jsonPath("$.minorDepartment.entityId")
    .value(minorDepartmentEntityId.toString()));
```

Keep one primary-only POST test and assert both optional response fields are null:

```java
.andExpect(jsonPath("$.doubleMajorDepartment").doesNotExist())
.andExpect(jsonPath("$.minorDepartment").doesNotExist());
```

Add this 400 test where primary and minor IDs match:

```java
@Test
void 같은_학과를_여러_전공으로_등록하면_400을_반환한다() throws Exception {
    UUID authUserId = UUID.fromString("00000000-0000-0000-0000-000000000010");
    UUID departmentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    String email = "student@inha.edu";
    StudentProfileRegistrationRequest request = new StudentProfileRegistrationRequest(
        departmentId,
        null,
        departmentId,
        2024,
        EnrollmentStatus.ENROLLED,
        "인하"
    );
    when(studentService.registerProfile(authUserId, email, request))
        .thenThrow(new DuplicateMajorDepartmentException());

    mockMvc.perform(post("/api/v1/students/me")
            .with(jwt().jwt(token -> token.subject(authUserId.toString()).claim("email", email)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "primaryDepartmentEntityId": "00000000-0000-0000-0000-000000000001",
                  "minorDepartmentEntityId": "00000000-0000-0000-0000-000000000001",
                  "admissionYear": 2024,
                  "enrollmentStatus": "ENROLLED",
                  "nickname": "인하"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("DUPLICATE_MAJOR_DEPARTMENT"));
}
```

- [ ] **Step 9: Update `StudentController` OpenAPI registration and lookup examples**

Show `doubleMajorDepartmentEntityId` and `minorDepartmentEntityId` in the POST request example. Show nullable `doubleMajorDepartment` and `minorDepartment` in both POST and GET response examples. Add `DUPLICATE_MAJOR_DEPARTMENT` to the documented 400 responses.

- [ ] **Step 10: Run controller tests**

```bash
./gradlew integrationTest --tests '*StudentControllerTest'
```

Expected: PASS.

- [ ] **Step 11: Commit registration and read support**

```bash
git add src/main/java/com/ahni/backend/dto \
  src/main/java/com/ahni/backend/exception \
  src/main/java/com/ahni/backend/service/StudentService.java \
  src/main/java/com/ahni/backend/controller/StudentController.java \
  src/test/java/com/ahni/backend/service/StudentServiceTest.java \
  src/test/java/com/ahni/backend/controller/StudentControllerTest.java
git commit -m "feat(major): 학생 다중전공 등록 및 조회 지원"
```

### Task 4: Replace a Student's Complete Major Configuration

**Files:**
- Create: `src/main/java/com/ahni/backend/dto/StudentMajorUpdateRequest.java`
- Modify: `src/main/java/com/ahni/backend/service/StudentService.java`
- Modify: `src/main/java/com/ahni/backend/controller/StudentController.java`
- Modify: `src/test/java/com/ahni/backend/service/StudentServiceTest.java`
- Modify: `src/test/java/com/ahni/backend/controller/StudentControllerTest.java`

**Interfaces:**
- Produces: `StudentService.replaceMajors(UUID, StudentMajorUpdateRequest)`.
- Produces: authenticated `PUT /api/v1/students/me/majors` returning `StudentProfileResponse`.

- [ ] **Step 1: Add the update request record**

Create:

```java
package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StudentMajorUpdateRequest(
    @NotNull
    @Schema(description = "주전공 학과 식별자")
    UUID primaryDepartmentEntityId,

    @Schema(description = "복수전공 학과 식별자", nullable = true)
    UUID doubleMajorDepartmentEntityId,

    @Schema(description = "부전공 학과 식별자", nullable = true)
    UUID minorDepartmentEntityId
) {
}
```

- [ ] **Step 2: Write failing service tests for replacement**

Add one test that covers retaining, removing, and replacing assignments in the same transaction:

```java
@Test
void 학생의_전공_구성을_전체_교체한다() {
    UUID authUserId = UUID.randomUUID();
    Student student = new Student(
        authUserId,
        "student@inha.edu",
        2024,
        EnrollmentStatus.ENROLLED,
        "인하"
    );
    Department primary = new Department("소프트웨어융합공학과");
    Department oldDoubleMajor = new Department("금융투자학과");
    Department oldMinor = new Department("산업경영학과");
    Department newMinor = new Department("메카트로닉스공학과");
    StudentMajor primaryAssignment = new StudentMajor(student, primary, MajorType.PRIMARY);
    StudentMajor doubleMajorAssignment = new StudentMajor(
        student,
        oldDoubleMajor,
        MajorType.DOUBLE_MAJOR
    );
    StudentMajor minorAssignment = new StudentMajor(student, oldMinor, MajorType.MINOR);
    UUID primaryAssignmentEntityId = primaryAssignment.getEntityId();
    StudentMajorUpdateRequest request = new StudentMajorUpdateRequest(
        primary.getEntityId(),
        null,
        newMinor.getEntityId()
    );

    when(studentRepository.findByAuthUserId(authUserId)).thenReturn(Optional.of(student));
    when(departmentRepository.findByEntityId(primary.getEntityId()))
        .thenReturn(Optional.of(primary));
    when(departmentRepository.findByEntityId(newMinor.getEntityId()))
        .thenReturn(Optional.of(newMinor));
    when(studentMajorRepository.findAllByStudentAndDeletedAtIsNull(student))
        .thenReturn(List.of(primaryAssignment, doubleMajorAssignment, minorAssignment));

    StudentProfileResponse response = studentService.replaceMajors(authUserId, request);

    assertThat(primaryAssignment.getEntityId()).isEqualTo(primaryAssignmentEntityId);
    assertThat(primaryAssignment.getDeletedAt()).isNull();
    assertThat(doubleMajorAssignment.getDeletedAt()).isNotNull();
    assertThat(minorAssignment.getDeletedAt()).isNotNull();
    assertThat(response.doubleMajorDepartment()).isNull();
    assertThat(response.minorDepartment().entityId()).isEqualTo(newMinor.getEntityId());

    InOrder inOrder = inOrder(studentMajorRepository);
    inOrder.verify(studentMajorRepository).flush();
    inOrder.verify(studentMajorRepository).saveAll(anyList());
}
```

Add the duplicate-ID failure test:

```java
@Test
void 같은_학과를_여러_전공으로_변경할_수_없다() {
    UUID authUserId = UUID.randomUUID();
    UUID departmentEntityId = UUID.randomUUID();
    Student student = new Student(
        authUserId,
        "student@inha.edu",
        2024,
        EnrollmentStatus.ENROLLED,
        "인하"
    );
    StudentMajorUpdateRequest request = new StudentMajorUpdateRequest(
        departmentEntityId,
        null,
        departmentEntityId
    );
    when(studentRepository.findByAuthUserId(authUserId)).thenReturn(Optional.of(student));

    assertThatThrownBy(() -> studentService.replaceMajors(authUserId, request))
        .isInstanceOf(DuplicateMajorDepartmentException.class);

    verifyNoInteractions(departmentRepository, studentMajorRepository);
}
```

Add the missing-student failure test:

```java
@Test
void 등록되지_않은_학생은_전공을_변경할_수_없다() {
    UUID authUserId = UUID.randomUUID();
    StudentMajorUpdateRequest request = new StudentMajorUpdateRequest(
        UUID.randomUUID(),
        null,
        null
    );
    when(studentRepository.findByAuthUserId(authUserId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> studentService.replaceMajors(authUserId, request))
        .isInstanceOf(StudentNotFoundException.class);

    verifyNoInteractions(departmentRepository, studentMajorRepository);
}
```

Add the unavailable-department failure test. `Optional.empty()` represents both a missing identifier and the repository boundary used when an inactive department is rejected:

```java
@Test
void 사용할_수_없는_학과로_전공을_변경할_수_없다() {
    UUID authUserId = UUID.randomUUID();
    UUID departmentEntityId = UUID.randomUUID();
    Student student = new Student(
        authUserId,
        "student@inha.edu",
        2024,
        EnrollmentStatus.ENROLLED,
        "인하"
    );
    StudentMajorUpdateRequest request = new StudentMajorUpdateRequest(
        departmentEntityId,
        null,
        null
    );
    when(studentRepository.findByAuthUserId(authUserId)).thenReturn(Optional.of(student));
    when(departmentRepository.findByEntityId(departmentEntityId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> studentService.replaceMajors(authUserId, request))
        .isInstanceOf(DepartmentNotFoundException.class);

    verify(studentMajorRepository, never())
        .findAllByStudentAndDeletedAtIsNull(any());
    verify(studentMajorRepository, never()).flush();
    verify(studentMajorRepository, never()).saveAll(any());
}
```

- [ ] **Step 3: Verify replacement tests fail**

```bash
./gradlew test --tests '*StudentServiceTest'
```

Expected: FAIL because `replaceMajors` is absent.

- [ ] **Step 4: Implement transactional replacement**

Add:

```java
@Transactional
public StudentProfileResponse replaceMajors(
    UUID authUserId,
    StudentMajorUpdateRequest request
) {
    Student student = studentRepository.findByAuthUserId(authUserId)
        .orElseThrow(StudentNotFoundException::new);
    EnumMap<MajorType, Department> requested = resolveDepartments(
        request.primaryDepartmentEntityId(),
        request.doubleMajorDepartmentEntityId(),
        request.minorDepartmentEntityId()
    );
    List<StudentMajor> active = studentMajorRepository
        .findAllByStudentAndDeletedAtIsNull(student);
    List<StudentMajor> retained = new ArrayList<>();

    for (StudentMajor current : active) {
        Department requestedDepartment = requested.get(current.getMajorType());
        if (requestedDepartment != null && current.isAssignedTo(requestedDepartment)) {
            retained.add(current);
            requested.remove(current.getMajorType());
        } else {
            current.softDelete();
        }
    }

    studentMajorRepository.flush();

    List<StudentMajor> created = requested.entrySet().stream()
        .map(entry -> new StudentMajor(student, entry.getValue(), entry.getKey()))
        .toList();
    studentMajorRepository.saveAll(created);

    List<StudentMajor> result = new ArrayList<>(retained);
    result.addAll(created);
    return toResponse(student, result);
}
```

Resolve and validate all requested departments before soft-deleting any active assignment. This guarantees validation failures leave the current configuration unchanged.

- [ ] **Step 5: Run replacement service tests**

```bash
./gradlew test --tests '*StudentServiceTest'
```

Expected: PASS.

- [ ] **Step 6: Add the authenticated PUT controller test first**

Use `MockMvcRequestBuilders.put` and verify that the JWT subject, not a request student ID, is passed to the service:

```java
mockMvc.perform(put("/api/v1/students/me/majors")
        .with(jwt().jwt(token -> token.subject(authUserId.toString())))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "primaryDepartmentEntityId": "00000000-0000-0000-0000-000000000001",
              "doubleMajorDepartmentEntityId": null,
              "minorDepartmentEntityId": "00000000-0000-0000-0000-000000000003"
            }
            """))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.primaryDepartment.entityId")
        .value("00000000-0000-0000-0000-000000000001"))
    .andExpect(jsonPath("$.doubleMajorDepartment").doesNotExist())
    .andExpect(jsonPath("$.minorDepartment.entityId")
        .value("00000000-0000-0000-0000-000000000003"));
```

Add the unauthenticated and invalid-request boundary tests:

```java
@Test
void 인증되지_않은_사용자는_전공을_변경할_수_없다() throws Exception {
    mockMvc.perform(put("/api/v1/students/me/majors")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(studentService);
}

@Test
void 주전공이_없으면_전공을_변경할_수_없다() throws Exception {
    mockMvc.perform(put("/api/v1/students/me/majors")
            .with(jwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

    verifyNoInteractions(studentService);
}
```

Add one test for each service exception using a valid `StudentMajorUpdateRequest` and the same valid JSON as the success test:

```java
@Test
void 등록되지_않은_학생의_전공_변경은_404를_반환한다() throws Exception {
    UUID authUserId = UUID.fromString("00000000-0000-0000-0000-000000000010");
    StudentMajorUpdateRequest request = new StudentMajorUpdateRequest(
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        null,
        null
    );
    when(studentService.replaceMajors(authUserId, request))
        .thenThrow(new StudentNotFoundException());

    mockMvc.perform(put("/api/v1/students/me/majors")
            .with(jwt().jwt(token -> token.subject(authUserId.toString())))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"primaryDepartmentEntityId":"00000000-0000-0000-0000-000000000001"}
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
}
```

Add the missing-department boundary test:

```java
@Test
void 존재하지_않는_학과로_전공을_변경하면_404를_반환한다() throws Exception {
    UUID authUserId = UUID.fromString("00000000-0000-0000-0000-000000000010");
    StudentMajorUpdateRequest request = new StudentMajorUpdateRequest(
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        null,
        null
    );
    when(studentService.replaceMajors(authUserId, request))
        .thenThrow(new DepartmentNotFoundException());

    mockMvc.perform(put("/api/v1/students/me/majors")
            .with(jwt().jwt(token -> token.subject(authUserId.toString())))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"primaryDepartmentEntityId":"00000000-0000-0000-0000-000000000001"}
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("DEPARTMENT_NOT_FOUND"));
}
```

Add the duplicate-department boundary test:

```java
@Test
void 같은_학과를_여러_전공으로_변경하면_400을_반환한다() throws Exception {
    UUID authUserId = UUID.fromString("00000000-0000-0000-0000-000000000010");
    UUID departmentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    StudentMajorUpdateRequest request = new StudentMajorUpdateRequest(
        departmentId,
        null,
        departmentId
    );
    when(studentService.replaceMajors(authUserId, request))
        .thenThrow(new DuplicateMajorDepartmentException());

    mockMvc.perform(put("/api/v1/students/me/majors")
            .with(jwt().jwt(token -> token.subject(authUserId.toString())))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "primaryDepartmentEntityId": "00000000-0000-0000-0000-000000000001",
                  "minorDepartmentEntityId": "00000000-0000-0000-0000-000000000001"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("DUPLICATE_MAJOR_DEPARTMENT"));
}
```

- [ ] **Step 7: Add the controller operation and OpenAPI contract**

Add to `StudentController`:

```java
@PutMapping("/me/majors")
public StudentProfileResponse replaceMajors(
    @AuthenticationPrincipal Jwt jwt,
    @Valid @RequestBody StudentMajorUpdateRequest request
) {
    return studentService.replaceMajors(UUID.fromString(jwt.getSubject()), request);
}
```

Add `@Operation`, bearer security, request example, complete response example, and documented 400/401/404 responses matching the design spec.

- [ ] **Step 8: Run all student controller tests**

```bash
./gradlew integrationTest --tests '*StudentControllerTest'
```

Expected: PASS.

- [ ] **Step 9: Commit the replacement API**

```bash
git add src/main/java/com/ahni/backend/dto/StudentMajorUpdateRequest.java \
  src/main/java/com/ahni/backend/service/StudentService.java \
  src/main/java/com/ahni/backend/controller/StudentController.java \
  src/test/java/com/ahni/backend/service/StudentServiceTest.java \
  src/test/java/com/ahni/backend/controller/StudentControllerTest.java
git commit -m "feat(major): 학생 전공 구성 변경 API 추가"
```

### Task 5: Publish the Generated API and Domain Contract

**Files:**
- Modify: `docs/api/openapi.json`
- Modify: `docs/domain/index.md`

**Interfaces:**
- Consumes: runtime springdoc output from Tasks 3 and 4.
- Produces: checked-in API contract consumed by mobile contract tests and explicit domain cardinality documentation.

- [ ] **Step 1: Run the OpenAPI drift test and capture the generated contract**

```bash
./gradlew integrationTest --tests '*OpenApiContractTest.generatedContractMatchesCheckedInContract'
```

Expected: FAIL because `docs/api/openapi.json` still describes the primary-only profile. The test writes the current runtime contract to `build/openapi/openapi.json`.

- [ ] **Step 2: Replace the checked-in contract with generated output**

```bash
cp build/openapi/openapi.json docs/api/openapi.json
```

Do not hand-edit generated JSON.

- [ ] **Step 3: Document the domain cardinalities**

Update the `student_major` entry in `docs/domain/index.md` to state:

```markdown
- `student_major`: exactly one active primary major and at most one active double major and minor per student. "Multiple majors" is the product umbrella for double-major and minor assignments, not a stored major type.
```

- [ ] **Step 4: Verify the generated API contract matches**

```bash
./gradlew integrationTest --tests '*OpenApiContractTest'
git diff --check
```

Expected: both commands PASS.

- [ ] **Step 5: Commit generated and human-readable contracts**

```bash
git add docs/api/openapi.json docs/domain/index.md
git commit -m "docs(major): 다중전공 API 계약 문서화"
```

### Task 6: Run the Full Backend Gate and Push

**Files:**
- Verify all files changed in Tasks 1-5.

**Interfaces:**
- Produces: a review-ready `feat/student-major-management` branch.

- [ ] **Step 1: Run the complete harness**

```bash
./scripts/verify
```

Expected: `BUILD SUCCESSFUL`; unit, architecture, MVC, OpenAPI, repository, and PostgreSQL migration tests all pass.

- [ ] **Step 2: Inspect the complete feature diff**

```bash
git status --short
git diff --check origin/main...HEAD
git log --oneline origin/main..HEAD
```

Expected: clean worktree, no whitespace errors, and five atomic commits matching Tasks 1-5.

- [ ] **Step 3: Push the feature branch**

```bash
git push -u origin feat/student-major-management
```

- [ ] **Step 4: Record the mobile handoff**

After the backend PR is merged, create a separate mobile implementation plan against the merged `docs/api/openapi.json`. That plan must cover registration selectors, profile rendering, complete replacement, API serialization, controller tests, widget tests, and user-run simulator/device verification.
