ALTER TABLE public.graduation_requirement
    RENAME COLUMN min_major_credit TO min_department_credit;

ALTER TABLE public.graduation_requirement
    RENAME CONSTRAINT ck_graduation_requirement_major_credit
        TO ck_graduation_requirement_department_credit;

ALTER TABLE public.graduation_requirement
    ADD COLUMN min_general_credit numeric(5,1) NOT NULL DEFAULT 0.0,
    ADD CONSTRAINT ck_graduation_requirement_general_credit
        CHECK (min_general_credit >= 0.0);

UPDATE public.graduation_requirement
SET min_department_credit = min_double_major_credit
WHERE major_type = 'DOUBLE_MAJOR';

ALTER TABLE public.graduation_requirement
    DROP CONSTRAINT ck_graduation_requirement_double_major_credit,
    DROP COLUMN min_double_major_credit,
    ALTER COLUMN min_general_credit DROP DEFAULT;
