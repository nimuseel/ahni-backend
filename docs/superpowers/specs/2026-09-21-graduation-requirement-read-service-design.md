# Graduation Requirement Read Service Design

**Status:** Approved implementation slice

**Date:** 2026-09-21

## Scope

This slice assembles the graduation policies and active required courses for
the authenticated student's active majors. It does not calculate completion,
missing credits, or graduation eligibility, and it does not expose an HTTP
endpoint yet.

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

- the graduation requirement's external identifier and thresholds;
- its department, admission year, and major type;
- active required-course relations in course-code order;
- each course's stable catalog data and requirement-specific category.

The response does not expose database identifiers. Required-course category
remains separate from the broad course catalog category.

## Deferred work

- completed-course comparison and missing-course calculation;
- credit and graduation-status analysis;
- authenticated HTTP endpoint and OpenAPI contract;
- mobile presentation;
- administrator policy management and production policy seeding.
