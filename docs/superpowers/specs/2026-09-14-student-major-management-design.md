# Student Major Management Design

**Status:** Approved scope

**Date:** 2026-09-14

## Goal

Allow a student to register and manage exactly one primary major, at most one double major, and at most one minor. Use "multiple majors" as the product-facing umbrella term rather than as a separate major type.

## Source Requirements

- `O2_usecase_spec_v1.0.pdf` UC-S04 requires required-course and graduation-requirement views to include multiple majors and separate results by major.
- `O2_mini_spec_v1.0.pdf` sections 2.7 and 3.1 require graduation checks and course guidance to evaluate each major independently.
- `O2_database_diagram.png` distinguishes a primary major, double major, and minor.
- The approved scope is one primary major, zero or one double major, and zero or one minor.

Approved decisions take precedence over older source details that conflict with the current identity and profile policy.

## Terminology

| Product term | Domain value | Cardinality |
| --- | --- | --- |
| 주전공 | `PRIMARY` | Exactly one active assignment |
| 복수전공 | `DOUBLE_MAJOR` | Zero or one active assignment |
| 부전공 | `MINOR` | Zero or one active assignment |
| 다중전공 | No enum value | UI umbrella for double major and minor |

`MULTIPLE_MAJOR` must not be added to `MajorType`. It would describe a category of assignments rather than the academic relationship represented by one row.

## Current State

The normalized `student_major` table and `StudentMajor` entity already represent a student-to-department relationship with `PRIMARY`, `DOUBLE_MAJOR`, and `MINOR` types. The current implementation has these gaps:

- Registration accepts only `primaryDepartmentEntityId`.
- Profile responses expose only `primaryDepartment`.
- The database limits active primary majors to one, but does not limit active double majors or minors to one each.
- The database can represent the same active department under different major types.
- There is no API for replacing a student's major configuration after registration.

## Chosen Approach

Keep `student_major` as the only student-major relation. Strengthen its active-row constraints, extend registration and profile contracts with explicit optional double-major and minor fields, and add one replacement endpoint for later corrections.

This is preferred over storing department foreign keys on `student`, creating one table per major type, or introducing a generic academic-program model. Those alternatives either duplicate the existing relation or add scope not required by the current plan.

## Data Model

No new table is required.

An active assignment is a `student_major` row whose `deleted_at` is null. The database must enforce:

1. A student has at most one active row for each `major_type`.
2. A student cannot use the same department in more than one active major assignment.
3. `major_type` remains limited to `PRIMARY`, `DOUBLE_MAJOR`, and `MINOR`.
4. Application flows that create a profile must always create one active `PRIMARY` row.

The database can enforce "at most one" but not "exactly one" without coupling separate writes. The service transaction guarantees that registration and major replacement finish with one primary major.

A new Flyway migration will replace the current active-assignment and primary-only unique indexes with:

```sql
CREATE UNIQUE INDEX uk_student_major_active_type
    ON public.student_major (student_id, major_type)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_student_major_active_department
    ON public.student_major (student_id, department_id)
    WHERE deleted_at IS NULL;
```

The migration must run only after an integration test proves that existing V1-V9 data migrates successfully and both new constraints reject invalid active assignments.

## Domain Rules

The major configuration is valid only when:

- A primary department is present.
- Every referenced department exists and is active.
- Primary, double-major, and minor department identifiers are pairwise distinct.
- Double major and minor are optional.
- Major ownership comes from the authenticated student's JWT subject; clients never submit a student identifier.

No application-level maximum beyond one assignment per supported type is needed.

## HTTP API Contract

### Register student profile

Keep the existing endpoint:

```http
POST /api/v1/students/me
```

Extend the request without renaming existing fields:

```json
{
  "primaryDepartmentEntityId": "00000000-0000-0000-0000-000000000001",
  "doubleMajorDepartmentEntityId": "00000000-0000-0000-0000-000000000002",
  "minorDepartmentEntityId": null,
  "admissionYear": 2024,
  "enrollmentStatus": "ENROLLED",
  "nickname": "인하"
}
```

The two new fields are optional. Existing clients that send only `primaryDepartmentEntityId` remain valid.

### Read student profile

Keep the existing endpoint:

```http
GET /api/v1/students/me
```

Extend the response with nullable department objects:

```json
{
  "studentEntityId": "00000000-0000-0000-0000-000000000020",
  "email": "student@inha.edu",
  "nickname": "인하",
  "primaryDepartment": {
    "entityId": "00000000-0000-0000-0000-000000000001",
    "name": "소프트웨어융합공학과"
  },
  "doubleMajorDepartment": {
    "entityId": "00000000-0000-0000-0000-000000000002",
    "name": "산업경영학과"
  },
  "minorDepartment": null,
  "admissionYear": 2024,
  "enrollmentStatus": "ENROLLED",
  "accountStatus": "ACTIVE"
}
```

### Replace major configuration

Add:

```http
PUT /api/v1/students/me/majors
```

Request:

```json
{
  "primaryDepartmentEntityId": "00000000-0000-0000-0000-000000000001",
  "doubleMajorDepartmentEntityId": null,
  "minorDepartmentEntityId": "00000000-0000-0000-0000-000000000003"
}
```

The request is a complete replacement, not a patch. Omitting or sending null for an optional type removes that assignment. The response is the complete `StudentProfileResponse` so the client can replace its local profile state with the server result.

The update runs in one transaction. It reuses an unchanged assignment, soft-deletes a removed or replaced assignment, and creates the requested replacement. This preserves stable assignment identifiers where no change occurred and avoids insert-before-soft-delete unique-index failures.

## Error Contract

Use the existing `ApiErrorResponse` shape.

| HTTP status | Code | Condition |
| --- | --- | --- |
| `400` | `DUPLICATE_MAJOR_DEPARTMENT` | The same department is selected for two or more major types |
| `400` | `INVALID_REQUEST` | A required field is missing or malformed |
| `401` | Existing security response | The JWT is missing or invalid |
| `404` | `STUDENT_NOT_FOUND` | Major replacement is requested before profile registration |
| `404` | `DEPARTMENT_NOT_FOUND` | Any selected department does not exist or is inactive |

Database unique constraints remain the final concurrency guard. Expected validation failures must be detected before persistence so clients receive stable domain errors rather than raw constraint messages.

## Mobile Experience

### Registration

Keep the current primary-major selector required. Add a "다중전공" section below it with:

- Optional 복수전공 selector
- Optional 부전공 selector
- A clear action for each optional selection

Each selector uses the existing department list. Once a department is selected for one type, it is disabled in the other selectors. Client validation improves feedback, while the backend remains authoritative.

### Profile management

Display the three types separately. The edit screen submits the complete major configuration to `PUT /api/v1/students/me/majors`. A successful response replaces the in-memory profile. A failed request keeps the user's selections and displays the backend-safe message.

UI copy uses "다중전공" only as a section heading. Individual values are labeled "복수전공" and "부전공".

## Graduation and Course-Guidance Boundary

This change records the student's major configuration only. It does not implement graduation calculations, required-course lookup, or recommendations.

Later academic-policy features must iterate over all active `student_major` assignments and evaluate requirements using each assignment's department, admission year, and major type. Requirement data may differ between primary, double-major, and minor even when the department is the same.

## Testing Strategy

### Backend

- Migration integration tests for one active row per type and one active row per department.
- Entity tests for soft deletion and type/department replacement behavior introduced by the implementation.
- Service tests for primary-only, all-three-type, duplicate-department, inactive-department, and replacement flows.
- Controller tests for request validation, authentication, response serialization, and stable errors.
- OpenAPI contract tests covering the nullable fields and `PUT /students/me/majors` operation.
- Repository integration tests proving all active majors are loaded deterministically by type.

### Mobile

- API serialization tests for omitted and populated optional majors.
- Controller tests for registration and complete replacement.
- Widget tests for optional selectors, duplicate-selection prevention, clearing a selection, loading, error, and success states.
- The user performs final simulator and physical-device checks.

## Delivery Order

1. Backend migration and persistence constraints.
2. Backend registration and profile-read contract extension.
3. Backend major-replacement API and OpenAPI documentation.
4. Mobile API model and controller integration.
5. Mobile registration selectors.
6. Mobile profile display and major-editing screen.

Backend contract changes must be merged before the mobile branch depends on them.

## Out of Scope

- Convergence, interdisciplinary, micro, and student-designed programs
- More than one double major or more than one minor
- Major application, approval, rejection, or abandonment workflows
- Historical major-assignment audit UI
- Graduation, scholarship, or recommendation calculation
- Administrator editing of an individual student's majors

Supporting an academic program that is not represented by a department requires a separate `academic_program` design rather than another nullable column or enum value in this feature.
