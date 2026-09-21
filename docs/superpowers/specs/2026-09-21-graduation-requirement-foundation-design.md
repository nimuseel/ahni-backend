# Graduation Requirement Foundation Design

**Status:** Approved implementation slice

**Date:** 2026-09-21

## Scope

This slice stores graduation-policy thresholds by department, admission year,
and major type. It deliberately does not calculate graduation status, expose a
student API, or classify required courses yet.

The source ERD contains a graduation-requirement row with total, major, and
double-major credit thresholds. The current student-major policy additionally
requires `PRIMARY`, `DOUBLE_MAJOR`, and `MINOR` to be evaluated independently,
so `major_type` is included in the normalized key.

## Data Model

`graduation_requirement` contains:

- the owning department;
- the admission year to which the policy applies;
- the major type being evaluated;
- minimum total, major, and double-major credits;
- immutable external identity and audit timestamps.

The database allows one policy for each `(department_id, admission_year,
major_type)` tuple. Credit values are non-negative decimal values with one
fractional digit. The entity applies the same boundary rules before persistence.

## Dependency Boundary

```text
department + admission year + major type
                         -> graduation requirement policy
course + student grade   -> later graduation analysis
```

The repository currently supports exact policy lookup and deterministic policy
listing for one department. A later service will select the policy for each
active student-major assignment and combine it with required-course rows and
the student's grade history.

## Deliberately Deferred

- `required_course` persistence and requirement-category rules;
- student graduation-status calculation;
- authenticated graduation API and mobile screen;
- administrator CRUD and initial production policy seeding;
- scholarship, recommendation, and course-simulation behavior.
