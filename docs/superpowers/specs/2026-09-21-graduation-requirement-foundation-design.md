# Graduation Requirement Foundation Design

**Status:** Approved implementation slice

**Date:** 2026-09-21

## Scope

This slice stores graduation-policy thresholds by department, admission year,
and major type. It deliberately does not calculate graduation status, expose a
student API, or classify required courses yet.

The source ERD contains a graduation-requirement row with total, major, and
double-major credit thresholds. The detailed mini specification additionally
requires a general-education threshold and independent evaluation of
`PRIMARY`, `DOUBLE_MAJOR`, and `MINOR`. The normalized model therefore uses
`major_type` in the policy key and one department-credit threshold per policy.

## Data Model

`graduation_requirement` contains:

- the owning department;
- the admission year to which the policy applies;
- the major type being evaluated;
- minimum total, department, and general-education credits;
- the title of the official source and an optional source URL;
- immutable external identity and audit timestamps.

The database allows one policy for each `(department_id, admission_year,
major_type)` tuple. Credit values are non-negative decimal values with one
fractional digit. The entity applies the same boundary rules before persistence.
`min_department_credit` means the credit threshold for the row's `major_type`;
it does not carry a second, unrelated major threshold.

The corrected threshold semantics and V16 migration are recorded in
[Graduation Credit Threshold Alignment](2026-09-22-graduation-credit-threshold-alignment-design.md).

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
- administrator policy deletion and initial production policy seeding;
- scholarship, recommendation, and course-simulation behavior.

## Administrator management

An administrator manually reads the university's official graduation-policy
material and registers one policy with its required-course assignments in a
single transaction. Registration and replacement are exposed at
`/api/v1/admin/graduation-requirements`. The Supabase JWT subject must match an
active `admin.auth_user_id`; authenticated non-administrators receive the
stable `ADMIN_ACCESS_DENIED` response.

Policy identity (`department`, `admission_year`, `major_type`) is immutable
after registration. An update replaces credit thresholds, source provenance,
and the complete active required-course assignment set. The source title is
required even when the source has no public URL.
