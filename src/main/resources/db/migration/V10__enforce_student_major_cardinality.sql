DROP INDEX public.uk_student_major_active_assignment;
DROP INDEX public.uk_student_major_active_primary;

CREATE UNIQUE INDEX uk_student_major_active_type
    ON public.student_major (student_id, major_type)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_student_major_active_department
    ON public.student_major (student_id, department_id)
    WHERE deleted_at IS NULL;
