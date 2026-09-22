ALTER TABLE public.graduation_requirement
    ADD COLUMN source_title varchar(200) NOT NULL DEFAULT '미등록',
    ADD COLUMN source_url varchar(2048);

ALTER TABLE public.graduation_requirement
    ALTER COLUMN source_title DROP DEFAULT;
