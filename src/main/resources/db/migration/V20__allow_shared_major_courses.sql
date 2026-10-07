-- Administrative affiliation is optional; annual curricula own recognition.
-- Preserve existing course identities, affiliations, category checks and FKs.
ALTER TABLE public.course DROP CONSTRAINT IF EXISTS ck_course_major_department;
