# Required Course Foundation Design

**Status:** Approved implementation slice

**Date:** 2026-09-21

## Scope

This slice models the relationship between a `graduation_requirement` policy and
the `course` records required by that policy. It does not calculate graduation
status, expose an HTTP API, or implement administrator workflows.

The course catalog design explicitly names the requirement-specific categories
`major-foundation`, `major-required`, and `general-required`. The category is
therefore owned by this relation, not by `CourseCategory`, which remains the
broad catalog category (`MAJOR`, `GENERAL_EDUCATION`, or `ELECTIVE`).

## Data model

`required_course` contains:

- `graduation_requirement_id`: the department, admission-year, and major-type policy;
- `course_id`: the shared course catalog record;
- `category`: `MAJOR_FOUNDATION`, `MAJOR_REQUIRED`, or `GENERAL_REQUIRED`;
- `deleted_at`: nullable soft-delete timestamp.

An active `(graduation_requirement_id, course_id)` pair is unique. Changing a
course's classification requires soft-deleting the old relation and creating a
new active relation, preserving the previous policy assignment.

## Persistence rules

- Both referenced entities are required and use lazy `ManyToOne` associations.
- Active required courses are returned in course-code order.
- Soft-deleted relations are excluded from the active repository query.
- PostgreSQL owns the active duplicate constraint and the category check.

## Follow-up

The next slice can add a read service that selects the applicable graduation
requirement for a student's admission year and active major, then compares the
student's completed course attempts with this relation.
