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
- `student_grade`: one student course attempt identified by academic year and term. Credit and grade point are immutable attempt snapshots; RPL has no grade code or grade point and is excluded from future GPA calculations.
- `admin`: administrator profile linked to a Supabase user ID for management functions.
- `allowed_signup_email_domain`: exact school domains accepted by the Supabase before-user-created hook.

Course category describes the broad academic area (`MAJOR`, `GENERAL_EDUCATION`, or `ELECTIVE`). It does not say whether a course is required for graduation. A future graduation-requirement relation owns required-course classification because that rule varies by department and admission year. Student grades, graduation analysis, and course guidance must reference the shared course catalog instead of duplicating course code or name. The grade attempt preserves submitted credit and grade point as historical snapshots.

Grade history currently records attempts and deterministic grade-point conversion only. GPA aggregation, OCR import, and retake replacement calculations remain planned consumers and must not be inferred from the `is_retake` marker alone.

Supabase email confirmation is the signup gate. `PENDING` describes a Supabase user before email confirmation and is not a persisted student account status. The backend creates a student profile only from an authenticated JWT and derives ownership from its subject and email claims.

Email confirmation proves control of an allowed address, not current enrollment. Enrollment status is initially self-reported and may influence academic guidance and notification policy. Do not introduce certificate upload, administrator approval, or external enrollment verification without a new approved decision.
