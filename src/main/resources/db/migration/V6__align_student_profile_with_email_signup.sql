DROP TABLE public.student_verification;

ALTER TABLE public.student RENAME COLUMN name to nickname;

ALTER TABLE public.student
    ALTER COLUMN nickname DROP NOT NULL,
    ADD COLUMN admission_year smallint NOT NULL,
    ALTER COLUMN account_status SET DEFAULT 'ACTIVE';

ALTER TABLE public.student DROP CONSTRAINT ck_student_account_status;

ALTER TABLE public.student ADD CONSTRAINT ck_student_account_status CHECK (account_status IN ('ACTIVE', 'SUSPENDED'));