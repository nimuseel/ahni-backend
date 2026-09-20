ALTER TABLE public.student_grade
    ADD COLUMN replaced_grade_id bigint;

ALTER TABLE public.student_grade
    ADD CONSTRAINT fk_student_grade_replaced_grade
        FOREIGN KEY (replaced_grade_id)
            REFERENCES public.student_grade (id),
    ADD CONSTRAINT uk_student_grade_replaced_grade
        UNIQUE (replaced_grade_id),
    ADD CONSTRAINT ck_student_grade_rpl_replacement
        CHECK (NOT is_rpl OR replaced_grade_id IS NULL);
