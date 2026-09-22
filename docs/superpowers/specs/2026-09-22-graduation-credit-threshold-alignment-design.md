# Graduation Credit Threshold Alignment

**Status:** Approved and implemented

**Date:** 2026-09-22

## Decision

Each graduation policy is already identified by department, admission year,
and major type. Its canonical credit thresholds are therefore:

- `min_total_credit`: minimum total completed credits;
- `min_department_credit`: minimum credits for the policy row's department and
  major type;
- `min_general_credit`: minimum general-education credits.

The model does not keep a separate `min_double_major_credit`. A
`DOUBLE_MAJOR` row represents that policy directly, while `PRIMARY` and
`MINOR` rows represent their own policies.

## Migration

V16 renames `min_major_credit` to `min_department_credit`, adds
`min_general_credit`, and removes `min_double_major_credit`. Before removal,
the migration copies every `DOUBLE_MAJOR` row's former
`min_double_major_credit` value into `min_department_credit`. Existing rows
receive a general-education threshold of `0.0`; policy data must be populated
with the actual curriculum value before graduation completion is calculated.

The migration test applies V1 through V15, inserts a legacy double-major
policy, applies V16, and verifies both value preservation and removal of the
legacy columns.

## API Contract

`GraduationRequirementResponse` exposes `minTotalCredit`,
`minDepartmentCredit`, and `minGeneralCredit`. The former
`minMajorCredit` and `minDoubleMajorCredit` properties are removed because this
API has not been released as a stable external contract.
