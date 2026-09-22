# Domain Map

AHNI connects student academic records to decisions about the next semester.

- Academic records: courses, credits, grades, GPA, RPL, retakes.
- Graduation: entry-year and department-specific required courses and credit thresholds.
- Scholarship: GPA, credit, and warning criteria.
- Timetable: lectures, conflict detection, custom start times, alarms.
- Recommendations: prerequisite flow, graduation urgency, professor preference.

When a rule is implemented, document the invariant and add a unit test beside it. Criteria data is configuration; calculation results are derived state.

- `student`: local profile linked to a Supabase user ID, including self-reported enrollment state.
- `student_major`: exactly one active primary major and at most one active double major and minor per student. "Multiple majors" is the product umbrella for double-major and minor assignments, not a stored major type.
- `department`: academic department referenced by student and later course data.
- `course`: active catalog entry with a globally unique uppercase code, normalized name, exact decimal credit, broad category, and optional department. `MAJOR` courses require a department; shared general-education and elective courses may omit it.
- `student_grade`: one student course attempt identified by academic year and term. A student may correct or delete only their own manually managed attempt; the associated course does not change during an update. Credit and grade point remain attempt snapshots, and RPL has no grade code or grade point and is excluded from GPA calculations.
- `graduation_requirement`: a department, admission-year, and major-type policy row containing minimum total, department, and general-education credit thresholds. The department threshold applies to the row's major type, so primary, double-major, and minor requirements remain independent. It is policy data for later graduation analysis, not a calculated result.
- `required_course`: a graduation-requirement-specific relation to the shared course catalog, classified as major foundation, major required, or general required. Active assignments are unique; reclassification uses soft deletion.
- Graduation-requirement lookup evaluates every active student major independently using an exact department, admission-year, and major-type policy. Results are ordered primary, double major, then minor; missing policy data is an explicit error rather than an implicit fallback.
- Graduation credit progress applies the same effective-attempt and completion rules as GPA summaries. Total credit includes all completed courses, department credit includes completed `MAJOR` courses owned by the policy department, and general credit includes completed `GENERAL_EDUCATION` courses. Remaining credit is clamped to zero; this result alone is not a final graduation-eligibility decision.
- `admin`: administrator profile linked to a Supabase user ID for management functions.
- `allowed_signup_email_domain`: exact school domains accepted by the Supabase before-user-created hook.

Course category describes the broad academic area (`MAJOR`, `GENERAL_EDUCATION`, or `ELECTIVE`). It does not say whether a course is required for graduation. A future graduation-requirement relation owns required-course classification because that rule varies by department and admission year. Student grades, graduation analysis, and course guidance must reference the shared course catalog instead of duplicating course code or name. The grade attempt preserves submitted credit and grade point as historical snapshots.

Grade history records attempts, deterministic grade-point conversion, and an explicit link from a retake to the earlier attempt it replaces. Replacement must not be inferred from the legacy `is_retake` marker alone. GPA aggregation excludes explicitly replaced attempts, uses stored credit and grade-point snapshots, includes RPL and `P` in completed credits, excludes RPL, `P`, and `NP` from GPA, and includes `F` credit in the GPA denominator. The complete approved policy is recorded in [Grade Retake and GPA Policy Design](../superpowers/specs/2026-09-20-grade-retake-gpa-policy-design.md).

Manual grade deletion is a hard delete because no audit-retention requirement has been approved. Introducing OCR provenance or an audit history requires a separate persistence decision rather than overloading the current attempt row.

Supabase email confirmation is the signup gate. `PENDING` describes a Supabase user before email confirmation and is not a persisted student account status. The backend creates a student profile only from an authenticated JWT and derives ownership from its subject and email claims.

Email confirmation proves control of an allowed address, not current enrollment. Enrollment status is initially self-reported and may influence academic guidance and notification policy. Do not introduce certificate upload, administrator approval, or external enrollment verification without a new approved decision.
