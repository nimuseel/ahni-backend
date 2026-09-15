# Grade History Design

**Status:** Approved direction, pending written-spec review

**Date:** 2026-09-15

## Goal

Add an authenticated student grade-history foundation that records manual course attempts and exposes the current student's history. This foundation must support later GPA, graduation, scholarship, simulation, and OCR workflows without implementing those calculations yet.

## Source Requirements

- `O2_usecase_spec_v1.0.pdf` UC-S03 requires an authenticated student to add manually entered course name, credit, and grade data to existing enrollment history.
- `O2_usecase_spec_v1.0.pdf` UC-S03 requires OCR results to be reviewed before they are applied retroactively. OCR is a later producer of the same grade-history model, not a separate persistence model.
- `O2_mini_spec_v1.0.pdf` section 2.1 stores course code, credit, grade, enrollment term, and a separate RPL marker.
- `O2_mini_spec_v1.0.pdf` section 2.3 uses weighted grade points, reports total and category GPA, includes RPL credit in completed credit, and excludes RPL from GPA.
- `O2_database_diagram.png` links grade records to a student and course and includes semester, grade, credit, RPL, and retake data.
- The approved course-catalog design establishes `department -> course -> grade` and requires grade history to reference a stable course identity.

Approved identity decisions supersede older references to student-number ownership. The backend derives the student from the authenticated Supabase JWT subject. `admissionYear`, not a student number, is used by later graduation-policy selection.

## Dependency Position

```text
Supabase JWT -> student -> student_grade -> course
                              |
                              +-> later GPA and simulation
                              +-> later graduation and scholarship checks
                              +-> later OCR import
```

The course catalog and student profile already exist. This feature supplies the reusable grade-attempt records consumed by later calculations.

## Considered Approaches

### 1. Attempt records with credit and grade-point snapshots — chosen

Each record references a student and course but also preserves the credit and converted grade point that applied when the student entered that attempt.

Advantages:

- later edits to course credit do not rewrite historical GPA inputs;
- future institution-specific grading scales can change without changing stored history;
- manual entry and reviewed OCR import can share one model;
- repeated attempts remain distinct by academic year and term.

Trade-off: some values intentionally duplicate catalog or grading-policy data.

### 2. Reference only the current course and a grade enum

This is smaller, but changing course credit or a grade conversion would silently change past results. It does not satisfy the historical-data requirement.

### 3. Build institution and grading-scale configuration first

This best supports many universities immediately, but introduces institution ownership, scale versioning, and administrator workflows before the first university needs them. It is deferred. Snapshot fields leave a migration path without requiring that subsystem now.

## Scope

This feature implements:

- the `student_grade` table and JPA entity;
- academic-term and grade-code domain enums;
- manual registration for the authenticated student's own grade;
- listing the authenticated student's grade history;
- stable validation, conflict, and not-found errors;
- unit, PostgreSQL integration, MVC/security, architecture, and OpenAPI contract tests.

It does not implement GPA calculation, mutation of existing records, OCR upload, or administrator management.

## Data Model

### `student_grade`

| Column | Type | Rule |
| --- | --- | --- |
| `id` | `bigint` identity | Internal primary key |
| `entity_id` | `uuid` | External immutable identifier, unique |
| `student_id` | `bigint` | Required FK to `student(id)` |
| `course_id` | `bigint` | Required FK to `course(id)` |
| `academic_year` | `smallint` | `2000` through the current service year in application validation |
| `term` | `varchar(20)` | `FIRST`, `SUMMER`, `SECOND`, or `WINTER` |
| `grade_code` | `varchar(20)`, nullable | Required for non-RPL records; absent for RPL |
| `grade_point` | `numeric(3,2)`, nullable | Backend-derived snapshot; absent for `P`, `NP`, and RPL |
| `credit` | `numeric(4,1)` | Attempt credit snapshot, greater than `0.0` and at most `30.0` |
| `is_rpl` | `boolean` | RPL marker, defaults to `false` |
| `is_retake` | `boolean` | Student-declared retake marker, defaults to `false` |
| `created_at` | `timestamptz` | Creation instant |
| `updated_at` | `timestamptz` | Last-update instant |

The database enforces:

1. `entity_id` is unique.
2. student and course foreign keys are non-null.
3. `(student_id, course_id, academic_year, term)` is unique.
4. `academic_year` is between `2000` and `9999`; the entity additionally rejects future years based on `Asia/Seoul`.
5. `term` and `grade_code` use only supported values.
6. credit is greater than `0.0`, at most `30.0`, and stored with one decimal place.
7. grade point is either null or between `0.00` and `4.50`.
8. RPL has no grade code or grade point.
9. non-RPL records require a grade code. Letter grades require a grade point; `P` and `NP` require a null grade point.

There is no `deleted_at` in this slice because update and deletion semantics have not been approved. A later mutation feature must choose hard deletion, soft deletion, or audit history before adding a delete endpoint.

## Domain Model

### `AcademicTerm`

```java
public enum AcademicTerm {
    FIRST(1),
    SUMMER(2),
    SECOND(3),
    WINTER(4)
}
```

The sequence supports deterministic newest-first sorting within an academic year. Persistence uses enum names, not ordinals.

### `GradeCode`

| Code | Grade point snapshot |
| --- | ---: |
| `A_PLUS` | `4.50` |
| `A_ZERO` | `4.00` |
| `B_PLUS` | `3.50` |
| `B_ZERO` | `3.00` |
| `C_PLUS` | `2.50` |
| `C_ZERO` | `2.00` |
| `D_PLUS` | `1.50` |
| `D_ZERO` | `1.00` |
| `F` | `0.00` |
| `P` | null |
| `NP` | null |

The phase-one conversion follows the 4.5 scale described in the supplied mini specification. The backend derives `gradePoint`; clients never submit it. The point is persisted as a snapshot so a later institution-specific grading policy can produce different values without rewriting earlier records.

### `StudentGrade`

The constructor accepts `Student`, `Course`, academic year, term, nullable grade code, credit, RPL status, and retake status.

Entity invariants:

- student and course are required;
- academic year is `2000 <= year <= current year` in `Asia/Seoul`;
- term is required;
- credit is required, greater than zero, at most `30.0`, and has at most one meaningful decimal place;
- a non-RPL record requires a grade code;
- an RPL record rejects a grade code and stores a null grade point;
- a non-RPL grade point is derived only from `GradeCode`;
- timestamps use `Instant` and the external ID uses UUID.

`isRetake` is metadata in this slice. It does not remove or replace an earlier attempt. The later GPA feature must define which attempts contribute to GPA and validate automatic retake detection before calculation.

## Repository Rules

The repository provides:

```java
boolean existsByStudentAndCourseAndAcademicYearAndTerm(
    Student student,
    Course course,
    int academicYear,
    AcademicTerm term
);

List<StudentGrade> findAllByStudent(Student student);
```

The list query uses an entity graph for `course` and `course.department` to avoid per-row lazy-loading queries. The service sorts records by academic year descending, term sequence descending, and course code ascending.

The course repository adds an active external-ID lookup:

```java
Optional<Course> findByEntityIdAndActiveTrue(UUID entityId);
```

## Service Flow

### Register a grade

1. Resolve the authenticated student by JWT subject; otherwise throw `StudentNotFoundException`.
2. Resolve an active course by external ID; otherwise throw `CourseNotFoundException`.
3. Convert and validate the request in the entity, deriving the grade-point snapshot server-side.
4. Reject an existing student/course/year/term tuple with `GradeAlreadyRegisteredException`.
5. Save the record and map it to a response DTO.

Manual entry selects an existing active course by `courseEntityId`; it does not create free-text course master data. The request supplies the transcript's credit value because a historical attempt may differ from the catalog's current credit. The backend derives the grade point and does not trust a client-supplied conversion.

### List grade history

1. Resolve the authenticated student.
2. Load that student's records only.
3. Sort newest academic year and term first, then course code ascending.
4. Return DTOs; no GPA or aggregate fields are calculated.

## HTTP API

Both endpoints require `Authorization: Bearer <supabase-jwt>`.

### Register a manual grade

```http
POST /api/v1/grades
Content-Type: application/json
```

Request:

```json
{
  "courseEntityId": "00000000-0000-0000-0000-000000000101",
  "academicYear": 2025,
  "term": "SECOND",
  "gradeCode": "A_PLUS",
  "credit": 3.0,
  "rpl": false,
  "retake": false
}
```

RPL request:

```json
{
  "courseEntityId": "00000000-0000-0000-0000-000000000102",
  "academicYear": 2025,
  "term": "FIRST",
  "gradeCode": null,
  "credit": 2.0,
  "rpl": true,
  "retake": false
}
```

Success returns `201 Created` and a `GradeResponse`.

`rpl` and `retake` default to `false` when omitted. `gradeCode` is required unless `rpl` is `true`.

### List my grade history

```http
GET /api/v1/grades
```

Response:

```json
[
  {
    "entityId": "00000000-0000-0000-0000-000000000201",
    "course": {
      "entityId": "00000000-0000-0000-0000-000000000101",
      "code": "CSE101",
      "name": "프로그래밍 기초",
      "category": "MAJOR",
      "department": {
        "entityId": "00000000-0000-0000-0000-000000000001",
        "name": "소프트웨어융합공학과"
      }
    },
    "academicYear": 2025,
    "term": "SECOND",
    "gradeCode": "A_PLUS",
    "gradePoint": 4.50,
    "credit": 3.0,
    "rpl": false,
    "retake": false,
    "createdAt": "2026-09-15T05:00:00Z",
    "updatedAt": "2026-09-15T05:00:00Z"
  }
]
```

`GradeCourseResponse` contains course identity, code, name, category, and nullable department, but not the catalog's current credit. The enclosing grade response exposes the historical credit snapshot, avoiding two competing credit fields.

An empty history returns `200 OK` with `[]`.

## Error Contract

Use the existing `ApiErrorResponse` shape.

| HTTP status | Code | Condition |
| --- | --- | --- |
| `400` | `INVALID_REQUEST` | Malformed JSON, UUID, enum, missing required value, or bean-validation failure |
| `400` | `INVALID_GRADE` | Cross-field grade, RPL, year, or credit invariant is invalid |
| `401` | Existing security response | JWT is absent or invalid |
| `404` | `STUDENT_NOT_FOUND` | The authenticated user has no student profile |
| `404` | `COURSE_NOT_FOUND` | The course does not exist or is inactive |
| `409` | `GRADE_ALREADY_REGISTERED` | The same course already exists for the academic year and term |

Database and stack-trace details are never returned to clients.

## Security

- The controller reads the Supabase JWT subject and never accepts a student identifier from the request.
- The service resolves all grade reads and writes through that authenticated student.
- A user cannot read or create another student's records through either endpoint.
- Course lookup allows active catalog entries only.
- No administrator-only behavior is added.

## Transaction and Concurrency

- Registration runs in one write transaction.
- Listing runs in a read-only transaction.
- The service-level existence check provides a clear conflict response for normal requests.
- The database unique constraint remains the concurrency backstop. A persistence conflict on the same tuple is translated to `GRADE_ALREADY_REGISTERED`, not exposed as a generic server error.

## Testing Strategy

### Entity tests

- grade-point conversion for every grade code;
- `P` and `NP` produce no grade point;
- RPL accepts no grade code and produces no grade point;
- non-RPL requires a grade code;
- student, course, year, term, and credit invariants;
- external ID and timestamp lifecycle through persistence tests.

### PostgreSQL integration tests

- V1-V12 migration success;
- JPA mapping and entity graph loading;
- duplicate attempt rejection;
- database rejection of invalid year, term, grade code, point range, credit, and RPL/grade combinations;
- records for different students, courses, or terms remain independent.

### Service tests

- ownership derived from auth user ID;
- active-course lookup;
- duplicate detection;
- server-side grade-point derivation;
- RPL mapping;
- newest-first deterministic ordering;
- missing student and course errors.

### Controller and contract tests

- authenticated registration returns `201`;
- authenticated listing returns values and `[]`;
- unauthenticated requests return `401`;
- stable `400`, `404`, and `409` errors;
- request and response examples, bearer security, nullable fields, and all error responses are present in OpenAPI;
- checked-in OpenAPI JSON matches the running application.

## Delivery Order

1. V12 migration and PostgreSQL constraint tests.
2. Academic-term and grade-code enums, entity, and unit tests.
3. Repository methods and persistence integration tests.
4. Request/response DTOs, exceptions, and service tests.
5. Controller, security, error mapping, and MVC tests.
6. OpenAPI, domain, traceability, and full harness verification.

## Out of Scope

- GPA, category GPA, completed-credit, scholarship, or simulation calculations
- Grade update or deletion
- Automatic retake detection or replacement policy
- OCR upload, recognition, review, or bulk confirmation
- Graduation requirements or required-course completion
- Administrator grade access
- Course creation, editing, deactivation, or production seeding
- Institution, college, or versioned grading-scale configuration
