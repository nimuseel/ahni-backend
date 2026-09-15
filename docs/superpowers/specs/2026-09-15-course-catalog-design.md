# Course Catalog Design

**Status:** Approved scope

**Date:** 2026-09-15

## Goal

Create the backend course catalog that later grade entry, graduation requirements, course guidance, and administrator course management can share.

## Source Requirements

- `O2_usecase_spec_v1.0.pdf` UC-S03 stores grade history against a course name, credit value, and grade.
- `O2_usecase_spec_v1.0.pdf` UC-S04 compares required courses with a student's completed courses and filters results by major and requirement category.
- `O2_usecase_spec_v1.0.pdf` UC-A02 manages course-code master data and requires a referenced course to be deactivated instead of deleted.
- `O2_mini_spec_v1.0.pdf` sections 2.1, 2.3, 2.7, 3.1, and 5.5 distinguish course master data from student grades and graduation-requirement criteria.
- `O2_database_diagram.png` models a course with a department, code, name, credit, type, and active state.

Approved identity decisions take precedence over older source wording. In particular, AHNI uses `admissionYear` rather than the removed student-number field when later selecting graduation requirements.

## Dependency Position

The catalog is implemented before grade history and graduation requirements because both reference a stable course identity. It is also implemented before administrator course CRUD so the future admin workflow extends an established entity and API contract instead of defining a second model.

```text
department -> course -> grade
                    -> required_course -> graduation_requirement
```

This feature delivers only the `course` node and its authenticated read API.

## Chosen Approach

Create a normalized `course` table, JPA entity, repository, service, response DTO, and authenticated list endpoint. The endpoint returns active courses and supports optional department and course-category filters.

Do not store required-course status on `course`. Whether a course is major-foundation, major-required, or general-required depends on the graduation requirement's department, admission year, and major type. A later `required_course` relation owns that classification.

## Data Model

### `course`

| Column | Type | Rule |
| --- | --- | --- |
| `id` | `bigint` identity | Internal primary key |
| `entity_id` | `uuid` | External immutable identifier, unique |
| `department_id` | `bigint`, nullable | Owning department; required for `MAJOR` courses |
| `code` | `varchar(30)` | Trimmed uppercase code, globally unique |
| `name` | `varchar(200)` | Non-blank display name |
| `credit` | `numeric(4,1)` | `0.0` through `30.0`, one decimal place |
| `category` | `varchar(30)` | `MAJOR`, `GENERAL_EDUCATION`, or `ELECTIVE` |
| `is_active` | `boolean` | Defaults to `true` |
| `created_at` | `timestamptz` | Creation instant |
| `updated_at` | `timestamptz` | Last-update instant |

The database enforces:

1. `entity_id` is unique.
2. `code` is unique and stored in uppercase.
3. `credit` is between `0.0` and `30.0` inclusive and has database scale `1`.
4. `category` is one of the three supported values.
5. A `MAJOR` course has a non-null `department_id`.

`GENERAL_EDUCATION` and `ELECTIVE` courses may have an owning department but do not require one. This supports shared courses without introducing a college or institution table in this feature.

## Domain Model

### `CourseCategory`

```java
public enum CourseCategory {
    MAJOR,
    GENERAL_EDUCATION,
    ELECTIVE
}
```

The enum describes the broad academic area used by grade summaries. It does not describe whether a course is mandatory.

### `Course`

The constructor accepts a nullable `Department`, code, name, credit, and category. It validates and normalizes values before persistence:

- code: trim, convert with `Locale.ROOT` to uppercase, reject blank or more than 30 characters;
- name: trim, reject blank or more than 200 characters;
- credit: non-null, scale at most one, `0.0 <= credit <= 30.0`;
- category: non-null;
- department: required when category is `MAJOR`.

No edit or deactivate methods are introduced until administrator course management needs them.

## Repository and Query Rules

The repository exposes only the queries needed by this feature:

- all active courses ordered by code;
- active courses for a department entity identifier ordered by code;
- active courses for a category ordered by code;
- active courses for both department and category ordered by code.

Filtering by department includes courses owned by that department only. It does not implicitly include shared general-education or elective courses. A client that needs every active course omits the department filter.

## HTTP API Contract

### List active courses

```http
GET /api/v1/courses
Authorization: Bearer <student-or-admin-jwt>
```

Optional query parameters:

| Parameter | Type | Meaning |
| --- | --- | --- |
| `departmentEntityId` | UUID | Only courses owned by the exact department |
| `category` | enum | Only `MAJOR`, `GENERAL_EDUCATION`, or `ELECTIVE` |

Example:

```http
GET /api/v1/courses?departmentEntityId=00000000-0000-0000-0000-000000000001&category=MAJOR
```

Response:

```json
[
  {
    "entityId": "00000000-0000-0000-0000-000000000101",
    "code": "CSE101",
    "name": "프로그래밍 기초",
    "credit": 3.0,
    "category": "MAJOR",
    "department": {
      "entityId": "00000000-0000-0000-0000-000000000001",
      "name": "소프트웨어융합공학과"
    }
  }
]
```

The `department` property is nullable for shared courses. Empty results return `200` with `[]`.

## Security

`GET /api/v1/courses` requires a valid Supabase JWT. The catalog is not required before signup, unlike the public department list. This feature does not distinguish student and administrator claims because it exposes only active, non-sensitive reference data.

Future mutation endpoints must require an administrator profile and are outside this feature.

## Error Contract

Use the existing `ApiErrorResponse` shape.

| HTTP status | Code | Condition |
| --- | --- | --- |
| `400` | `INVALID_REQUEST` | A query parameter is malformed |
| `400` | `INVALID_COURSE_CATEGORY` | `category` is not supported |
| `401` | Existing security response | JWT is absent or invalid |
| `404` | `DEPARTMENT_NOT_FOUND` | `departmentEntityId` does not identify an active department |

Unexpected persistence and infrastructure details are never returned to the client.

## Testing Strategy

### Migration and persistence

- V1-V11 migration success on PostgreSQL.
- Database rejection of lowercase codes, duplicate codes, invalid categories, invalid credit values, and major courses without a department.
- Repository filtering and deterministic code ordering.

### Entity

- code and name normalization;
- code, name, credit, category, and department invariants;
- timestamp and external identifier lifecycle.

### Service

- all four filter combinations;
- inactive courses excluded;
- inactive or missing department filter rejected;
- nullable department mapped safely.

### Controller and OpenAPI

- authenticated success and empty list;
- query-parameter forwarding;
- malformed UUID, invalid category, and missing department errors;
- response serialization including nullable department;
- operation summary, security requirement, parameters, examples, and stable error examples;
- checked-in OpenAPI contract updated from the running application.

## Delivery Order

1. V11 migration and migration integration tests.
2. Course category, entity, and entity tests.
3. Repository queries and PostgreSQL integration tests.
4. Service mapping and filtering tests.
5. Controller, security, stable errors, and OpenAPI documentation.
6. Checked-in OpenAPI refresh and full harness verification.

## Out of Scope

- Course create, update, deactivate, or administrator UI
- Administrator authentication and authorization
- Initial production course seeding
- Student grade history and GPA calculation
- OCR import
- Graduation requirements and required-course classification
- Course prerequisites, lectures, timetables, professor preferences, and recommendations
- Institution or college ownership models

