# Domain Map

AHNI connects student academic records to decisions about the next semester.

- Academic records: courses, credits, grades, GPA, RPL, retakes.
- Graduation: entry-year and department-specific required courses and credit thresholds.
- Scholarship: GPA, credit, and warning criteria.
- Timetable: lectures, conflict detection, custom start times, alarms.
- Recommendations: prerequisite flow, graduation urgency, professor preference.

When a rule is implemented, document the invariant and add a unit test beside it. Criteria data is configuration; calculation results are derived state.

- `student`: local student identity and enrollment state.
- `student_verification`: attempts to verify student status through an external source system.
- `department`: academic department owned by student and later course data.
- `admin`: administrator identity for management functions.

The initial verification flow must go through an external verification port. Do not mark a student as verified from a client-provided document without provider confirmation.
