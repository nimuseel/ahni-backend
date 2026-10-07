-- Complete eight officially documented shared-major links after V20.
-- Back up public first. Preserve all existing IDs, affiliations and grades.
BEGIN;
SET LOCAL lock_timeout='5s';
SET LOCAL statement_timeout='20s';
LOCK TABLE public.department,public.course,public.curriculum,public.curriculum_course IN SHARE ROW EXCLUSIVE MODE;
CREATE TEMP TABLE shared_targets(department_entity_id uuid,year integer,title text,url text,PRIMARY KEY(department_entity_id,year)) ON COMMIT DROP;
CREATE TEMP TABLE shared_links(department_entity_id uuid,year integer,code text,division text,recommended_year integer,recommended_term text,area_code text,area_name text,major_area text,note text,PRIMARY KEY(department_entity_id,year,code)) ON COMMIT DROP;
INSERT INTO shared_targets VALUES
('00000000-0000-0000-0000-000000000002',2025,'2025 메카트로닉스공학과 공식 교과과정표','https://sugang.inha.ac.kr/sugang/SU_51001/Curriculum_Xml.aspx?Value=3bqrAAQ7XUQF1T%252f3%252ftyHydLjf0dOCOH0NyZQJtx1iUk%253d'),
('00000000-0000-0000-0000-000000000003',2025,'2025 반도체산업융합학과 공식 교과과정표','https://sugang.inha.ac.kr/sugang/SU_51001/Curriculum_Xml.aspx?Value=3bqrAAQ7XUQF1T%252f3%252ftyHydifDI32IXsX9tHNMgZDNnE%253d'),
('00000000-0000-0000-0000-000000000002',2026,'2026 메카트로닉스공학과 공식 교과과정표','https://sugang.inha.ac.kr/sugang/SU_51001/Curriculum_Xml.aspx?Value=3bqrAAQ7XUQF1T%252f3%252ftyHyc8dfKqw3Fc7XoE0QYhoEp0%253d'),
('00000000-0000-0000-0000-000000000003',2026,'2026 반도체산업융합학과 공식 교과과정표','https://sugang.inha.ac.kr/sugang/SU_51001/Curriculum_Xml.aspx?Value=3bqrAAQ7XUQF1T%252f3%252ftyHyTm3fvOn1Z7Yxr85xGun0jU%253d');
INSERT INTO shared_links VALUES
('00000000-0000-0000-0000-000000000002',2025,'MTH1901','MAJOR_FOUNDATION',1,'FIRST',NULL,NULL,NULL,'공식 종별: 전공기초
권장이수: 1학년(1학기)
원본 비고: ⓐ 핵심 교양 GED1*** ~ GED6*** 중 각기 다른 영역에서 총 4개의 교과목 이수
또는
ⓑ 그래픽 디자인 이야기, 지속가능성을 위한 화학이야기, 동북아와 한일관계, 희곡의 이해, 생활과 과학, 영화로 보는 문학세계 중 택 4개 이수'),
('00000000-0000-0000-0000-000000000002',2025,'MTH1902','MAJOR_FOUNDATION',1,'SECOND',NULL,NULL,NULL,'공식 종별: 전공기초
권장이수: 1학년(2학기)
원본 비고: ⓐ 핵심 교양 GED1*** ~ GED6*** 중 각기 다른 영역에서 총 4개의 교과목 이수
또는
ⓑ 그래픽 디자인 이야기, 지속가능성을 위한 화학이야기, 동북아와 한일관계, 희곡의 이해, 생활과 과학, 영화로 보는 문학세계 중 택 4개 이수'),
('00000000-0000-0000-0000-000000000003',2025,'MTH1901','MAJOR_FOUNDATION',1,'FIRST',NULL,'전공기초',NULL,'공식 종별: 전공기초
권장이수: 1학년(1학기)
원본 비고: ⓐ 핵심 교양 GED1*** ~ GED6*** 중 각기 다른 영역에서 총 4개의 교과목 이수
또는
ⓑ 그래픽 디자인 이야기, 지속가능성을 위한 화학이야기, 동북아와 한일관계, 희곡의 이해, 생활과 과학, 영화로 보는 문학세계 중 택 4개 이수'),
('00000000-0000-0000-0000-000000000003',2025,'MTH1902','MAJOR_FOUNDATION',1,'SECOND',NULL,'전공기초',NULL,'공식 종별: 전공기초
권장이수: 1학년(2학기)
원본 비고: ⓐ 핵심 교양 GED1*** ~ GED6*** 중 각기 다른 영역에서 총 4개의 교과목 이수
또는
ⓑ 그래픽 디자인 이야기, 지속가능성을 위한 화학이야기, 동북아와 한일관계, 희곡의 이해, 생활과 과학, 영화로 보는 문학세계 중 택 4개 이수'),
('00000000-0000-0000-0000-000000000002',2026,'MTH1901','MAJOR_FOUNDATION',1,'FIRST',NULL,NULL,NULL,'공식 종별: 전공기초
권장이수: 1학년(1학기)
원본 비고: ⓐ 핵심 교양 GED1*** ~ GED6*** 중 각기 다른 영역에서 총 4개의 교과목 이수
또는
ⓑ 그래픽 디자인 이야기, 지속가능성을 위한 화학이야기, 동북아와 한일관계, 희곡의 이해, 생활과 과학, 영화로 보는 문학세계 중 택 4개 이수'),
('00000000-0000-0000-0000-000000000002',2026,'MTH1902','MAJOR_FOUNDATION',1,'SECOND',NULL,NULL,NULL,'공식 종별: 전공기초
권장이수: 1학년(2학기)
원본 비고: ⓐ 핵심 교양 GED1*** ~ GED6*** 중 각기 다른 영역에서 총 4개의 교과목 이수
또는
ⓑ 그래픽 디자인 이야기, 지속가능성을 위한 화학이야기, 동북아와 한일관계, 희곡의 이해, 생활과 과학, 영화로 보는 문학세계 중 택 4개 이수'),
('00000000-0000-0000-0000-000000000003',2026,'MTH1901','MAJOR_FOUNDATION',1,'FIRST',NULL,'전공기초',NULL,'공식 종별: 전공기초
권장이수: 1학년(1학기)
원본 비고: ⓐ 핵심 교양 GED1*** ~ GED6*** 중 각기 다른 영역에서 총 4개의 교과목 이수
또는
ⓑ 그래픽 디자인 이야기, 지속가능성을 위한 화학이야기, 동북아와 한일관계, 희곡의 이해, 생활과 과학, 영화로 보는 문학세계 중 택 4개 이수'),
('00000000-0000-0000-0000-000000000003',2026,'MTH1902','MAJOR_FOUNDATION',1,'SECOND',NULL,'전공기초',NULL,'공식 종별: 전공기초
권장이수: 1학년(2학기)
원본 비고: ⓐ 핵심 교양 GED1*** ~ GED6*** 중 각기 다른 영역에서 총 4개의 교과목 이수
또는
ⓑ 그래픽 디자인 이야기, 지속가능성을 위한 화학이야기, 동북아와 한일관계, 희곡의 이해, 생활과 과학, 영화로 보는 문학세계 중 택 4개 이수');
DO $$
BEGIN
 IF (SELECT count(*) FROM shared_targets)<>4 OR (SELECT count(*) FROM shared_links)<>8 THEN
   RAISE EXCEPTION 'Unexpected shared-major input size';
 END IF;
 IF EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.course'::regclass AND conname='ck_course_major_department') THEN
   RAISE EXCEPTION 'Apply V20 before importing shared majors';
 END IF;
 IF EXISTS(SELECT 1 FROM shared_targets i LEFT JOIN public.department d ON d.entity_id=i.department_entity_id
           LEFT JOIN public.curriculum cu ON cu.department_id=d.id AND cu.curriculum_year=i.year
           WHERE d.id IS NULL OR d.deleted_at IS NOT NULL OR cu.id IS NULL OR cu.published
              OR cu.source_title<>i.title OR cu.source_url IS DISTINCT FROM i.url) THEN
   RAISE EXCEPTION 'Target draft missing, published, deleted or different source';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM public.course WHERE code='MTH1901') THEN
   RAISE EXCEPTION 'Existing MTH1901 must be reused';
 END IF;
 IF EXISTS(SELECT 1 FROM public.course WHERE code IN ('MTH1901','MTH1902')
           AND (name<>CASE code WHEN 'MTH1901' THEN '일반수학 1' ELSE '일반수학 2' END
                OR credit<>3.0 OR category<>'MAJOR' OR NOT is_active
                OR (code='MTH1902' AND department_id IS NOT NULL))) THEN
   RAISE EXCEPTION 'Shared-major course identity conflicts; no overwrite';
 END IF;
 IF EXISTS(SELECT 1 FROM shared_links l JOIN public.department d ON d.entity_id=l.department_entity_id
           JOIN public.curriculum cu ON cu.department_id=d.id AND cu.curriculum_year=l.year
           JOIN public.course c ON c.code=l.code
           JOIN public.curriculum_course cc ON cc.curriculum_id=cu.id AND cc.course_id=c.id
           WHERE ROW(cc.division,cc.recommended_year,cc.recommended_term,cc.area_code,cc.area_name,cc.major_area,cc.note)
             IS DISTINCT FROM ROW(l.division,l.recommended_year,l.recommended_term,l.area_code,l.area_name,l.major_area,l.note)) THEN
   RAISE EXCEPTION 'Existing shared-major link differs; no replacement';
 END IF;
END $$;
INSERT INTO public.course(entity_id,code,name,credit,category,department_id,is_active,created_at,updated_at)
 SELECT '89333c17-8ba7-4b81-a353-b2f34c9d4d9f'::uuid,'MTH1902','일반수학 2',3.0,'MAJOR',NULL,true,now(),now()
 WHERE NOT EXISTS(SELECT 1 FROM public.course WHERE code='MTH1902');
WITH inserted AS (
 INSERT INTO public.curriculum_course(curriculum_id,course_id,division,recommended_year,recommended_term,area_code,area_name,major_area,note)
 SELECT cu.id,c.id,l.division,l.recommended_year,l.recommended_term,l.area_code,l.area_name,l.major_area,l.note
 FROM shared_links l JOIN public.department d ON d.entity_id=l.department_entity_id
 JOIN public.curriculum cu ON cu.department_id=d.id AND cu.curriculum_year=l.year
 JOIN public.course c ON c.code=l.code
 WHERE NOT EXISTS(SELECT 1 FROM public.curriculum_course cc WHERE cc.curriculum_id=cu.id AND cc.course_id=c.id)
 RETURNING curriculum_id
)
UPDATE public.curriculum SET version=version+1,updated_at=now() WHERE id IN (SELECT curriculum_id FROM inserted);
DO $$
BEGIN
 IF (SELECT count(*) FROM shared_links l JOIN public.department d ON d.entity_id=l.department_entity_id
     JOIN public.curriculum cu ON cu.department_id=d.id AND cu.curriculum_year=l.year
     JOIN public.course c ON c.code=l.code JOIN public.curriculum_course cc ON cc.curriculum_id=cu.id AND cc.course_id=c.id
     WHERE NOT cu.published AND ROW(cc.division,cc.recommended_year,cc.recommended_term,cc.area_code,cc.area_name,cc.major_area,cc.note)
       IS NOT DISTINCT FROM ROW(l.division,l.recommended_year,l.recommended_term,l.area_code,l.area_name,l.major_area,l.note))<>8 THEN
   RAISE EXCEPTION 'Shared-major post-insert verification failed';
 END IF;
END $$;
SELECT json_build_object('targetDrafts',4,'verifiedLinks',8,'published',false,
 'totalCourses',(SELECT count(*) FROM public.course),'totalLinks',(SELECT count(*) FROM public.curriculum_course));
COMMIT;
