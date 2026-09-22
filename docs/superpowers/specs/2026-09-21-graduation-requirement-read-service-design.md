# Graduation Requirement Read Service Design

**Status:** Approved implementation slice

**Date:** 2026-09-21

## Scope

This slice assembles and exposes the graduation policies and active required
courses for the authenticated student's active majors. It does not calculate
completion, missing credits, or graduation eligibility.

## Selection rules

For every active `student_major`, the service selects exactly one
`graduation_requirement` using:

- the student's `admission_year`;
- the major's department;
- the major's `major_type`.

The result order is `PRIMARY`, `DOUBLE_MAJOR`, then `MINOR`. Each major remains
an independent policy result, as required by the multiple-major design.

If any active major has no exact policy, the service raises
`GraduationRequirementNotFoundException` instead of returning incomplete or
silently substituted criteria. Policy fallback to another admission year is
not approved.

## Response boundary

Each result contains:

- the graduation requirement's external identifier and total, department, and
  general-education credit thresholds;
- its department, admission year, and major type;
- active required-course relations in course-code order;
- each course's stable catalog data and requirement-specific category.

The response does not expose database identifiers. Required-course category
remains separate from the broad course catalog category.

## HTTP contract

```http
GET /api/v1/graduation-requirements
Authorization: Bearer <student-jwt>
```

The endpoint returns `200` with one response per active major. Missing or
invalid authentication returns `401`. A missing student profile or an active
major without an exact graduation policy returns `404` with the shared
`ApiErrorResponse` shape. The generated OpenAPI document is the checked-in
client contract.

## Deferred work

- completed-course comparison and missing-course calculation;
- credit and graduation-status analysis;
- mobile presentation;
- administrator policy management and production policy seeding.
