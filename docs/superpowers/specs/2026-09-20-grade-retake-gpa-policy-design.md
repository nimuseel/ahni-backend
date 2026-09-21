# Grade Retake and GPA Policy Design

**Status:** Implemented

**Date:** 2026-09-20

## Goal

Define the grade-attempt replacement and GPA rules that must be settled before AHNI exposes a GPA summary. This document narrows the next implementation slice without adding GPA simulation, OCR, graduation, scholarship, or administrator behavior.

## Source Alignment

The mini specification section 2.3 requires:

- weighted GPA from course credits and converted grade points;
- overall GPA and category GPA for major, general-education, and elective courses;
- total completed credits;
- RPL credit in completed credits but not in GPA;
- `0.0` GPA and `0` completed credits when no history exists.

UC-S03 additionally requires the result to be recalculated when grade history changes. The source documents do not define how a retake identifies the attempt that it replaces. The current `is_retake` marker therefore cannot by itself decide which rows contribute to GPA.

Current approved identity, course, and grade-history decisions remain authoritative where the source documents describe superseded signup or persistence details.

## Retake Relationship

A retake must explicitly reference the earlier grade attempt it replaces. A boolean marker is not sufficient calculation input.

The grade model exposes an optional `replacedGradeEntityId` relationship with these rules:

1. The replacement and replaced attempts belong to the same student.
2. Both attempts reference the same course.
3. The replaced attempt is from an earlier academic period.
4. One attempt can be directly replaced by at most one later attempt.
5. A replacement chain is allowed, such as first attempt -> first retake -> second retake.
6. The chronological rule prevents cycles.
7. RPL records cannot replace another attempt and cannot be replaced as a retake.
8. A row referenced by a later attempt is excluded from GPA and completed-credit aggregation.

The legacy `is_retake` database column remains temporarily so existing development data is not destroyed without a backfill decision. New writes mirror whether `replacedGradeEntityId` is present, but the column is not accepted by the API, returned by the API, or used for calculations. A later migration may remove it only after existing `true` rows can be mapped or intentionally discarded.

## GPA Inputs

The calculator consumes the authenticated student's grade attempts after replaced attempts have been excluded. It uses each attempt's stored credit and grade-point snapshots rather than the current course credit or a newly calculated grade point.

| Grade kind | Completed credits | GPA numerator | GPA denominator |
| --- | --- | --- | --- |
| `A_PLUS` through `D_ZERO` | Included | `credit * gradePoint` | Credit included |
| `F` | Excluded | `0` | Credit included |
| `P` | Included | Excluded | Excluded |
| `NP` | Excluded | Excluded | Excluded |
| RPL | Included | Excluded | Excluded |

`GPA = sum(credit * gradePoint) / sum(GPA-applicable credit)`.

- GPA is rounded to two decimal places using `HALF_UP` only at the response boundary.
- Intermediate multiplication and division use `BigDecimal`; binary floating-point types are not used.
- When GPA-applicable credit is zero, GPA is `0.00`.
- Completed credits and GPA-applicable credits retain one decimal place.

## Summary Shape

The GPA endpoint returns a derived result and does not persist a summary table.

```json
{
  "gpa": 3.83,
  "completedCredits": 42.0,
  "gpaCredits": 36.0,
  "categories": [
    {
      "category": "MAJOR",
      "gpa": 4.02,
      "completedCredits": 24.0,
      "gpaCredits": 21.0
    }
  ]
}
```

The category list uses the existing `CourseCategory` values: `MAJOR`, `GENERAL_EDUCATION`, and `ELECTIVE`. Empty categories are returned with zero values so clients do not need to invent missing-state calculations.

The endpoint will be authenticated and owner-scoped:

```http
GET /api/v1/grades/summary
Authorization: Bearer <supabase-jwt>
```

Registration, correction, deletion, or retake-link changes are reflected on the next request. No cached aggregate is introduced in this phase.

## Scope Boundaries

This policy does not implement:

- target-GPA simulation or required grade combinations;
- semester-by-semester GPA;
- OCR import;
- graduation or scholarship decisions;
- institution-specific grading-scale configuration;
- administrator workflows.

The mini specification groups simulation with the current GPA result. AHNI separates it into a later feature because simulation additionally requires remaining-credit assumptions and a target-grade search policy that are not part of the current grade-history model.

## Required Test Boundaries

The implementation must protect at least these cases:

- credit-weighted GPA instead of an arithmetic mean of grades;
- `F` included in GPA credit but excluded from completed credit;
- `P`, `NP`, and RPL treatment from the table above;
- overall and category summaries;
- zero-history and zero-GPA-credit results;
- explicit retake replacement and multi-retake chains;
- rejection of cross-student, cross-course, same-period, future-period, duplicate, cyclic, and RPL replacement links;
- owner-scoped API access and OpenAPI response examples.
