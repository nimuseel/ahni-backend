# Domain Map

AHNI connects student academic records to decisions about the next semester.

- Academic records: courses, credits, grades, GPA, RPL, retakes.
- Graduation: entry-year and department-specific required courses and credit thresholds.
- Scholarship: GPA, credit, and warning criteria.
- Timetable: lectures, conflict detection, custom start times, alarms.
- Recommendations: prerequisite flow, graduation urgency, professor preference.

When a rule is implemented, document the invariant and add a unit test beside it. Criteria data is configuration; calculation results are derived state.

- `student`: local profile linked to a Supabase user ID, including self-reported enrollment state.
- `student_major`: the student's primary, double-major, or minor department relationship.
- `department`: academic department referenced by student and later course data.
- `admin`: administrator profile linked to a Supabase user ID for management functions.
- `allowed_signup_email_domain`: exact school domains accepted by the Supabase before-user-created hook.

Supabase email confirmation is the signup gate. `PENDING` describes a Supabase user before email confirmation and is not a persisted student account status. The backend creates a student profile only from an authenticated JWT and derives ownership from its subject and email claims.

Email confirmation proves control of an allowed address, not current enrollment. Enrollment status is initially self-reported and may influence academic guidance and notification policy. Do not introduce certificate upload, administrator approval, or external enrollment verification without a new approved decision.
