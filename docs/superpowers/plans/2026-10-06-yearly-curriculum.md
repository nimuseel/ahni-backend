# 연도별 교과과정 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. 사용자 지시에 따라 서브에이전트와 새 워크트리를 사용하지 않습니다.

**Goal:** 학생이 선택한 수강 연도에 공개된 교과과정의 과목만 조회·등록하도록 백엔드, 모바일, 관리자 화면을 연결합니다.

**Architecture:** 기존 course와 student_grade의 식별자·FK를 유지하고 curriculum과 curriculum_course를 추가합니다. 관리자 초안/공개 관리, 학생 연도별 조회, 성적 저장 검증을 기존 Controller → Service → Repository 구조로 구현합니다. 모바일은 서버의 연도별 결과만 검색합니다.

**Tech Stack:** 기존 Java 26, Spring Boot, JPA, Flyway, PostgreSQL/Testcontainers, Flutter, React/Vite/TypeScript, Vitest를 사용합니다. 새 의존성을 추가하지 않습니다.

**Spec:** [연도별 교과과정과 졸업요건 설계](../specs/2026-10-06-curriculum-graduation-policy-design.md)의 작업 단위 A.

## Global Constraints

- 수강 연도와 입학연도는 동일한 뜻으로 취급하지 않습니다.
- `course`의 기존 ID, 외부 entityId, 학수번호 유일성, 성적 FK는 유지합니다. 동일 이름·다른 학수번호의 과목은 합치지 않습니다.
- 기록 시간은 기존 Instant/timestamptz, 학점은 BigDecimal/numeric 규약을 유지합니다.
- 권장 정보를 실제 수강 가능 학기 제한으로 사용하지 않습니다.
- 해당 교과과정이 없으면 `404 CURRICULUM_NOT_AVAILABLE`입니다. 초안 내용은 학생에게 노출하지 않으며 다른 연도로 대체하지 않습니다.
- RPL에도 임의의 예외를 추가하지 않습니다.
- AHNI의 기존 색상, 폰트, 크기, WhitespaceWrappedText, 접근성과 loading/empty/error/retry 규칙을 유지합니다. 시뮬레이터·실기기 테스트는 사용자가 수행합니다.
- 공유 DB 적용, push, merge는 별도 작업입니다.

## Review Focus

1. 같은 이름·다른 코드가 다른 연도에 존재하는 경우: 선택 연도에 연결된 코드만 반환. Task 3에서 고정합니다.
2. 과거 비활성 과목: 공개 교과과정에 연결되어 있으면 과거 성적 등록 가능. Task 3에서 고정합니다.
3. 조회 중 연도를 다시 바꾼 경우: 이전 요청이 나중에 성공/실패해도 새 목록을 덮어쓰지 않음. Task 4에서 고정합니다.
4. 공개 편집과 성적 저장이 동시에 실행되는 경우: 검증 이후 초안으로 바뀐 내용을 잘못 저장하지 않음. Task 3에서 PostgreSQL 트랜잭션 테스트로 고정합니다.
5. 기존 성적의 연도 유지 수정: 교과과정 미등록으로 등급 수정까지 막지 않음; 연도 변경은 재검증. Task 3과 Task 4에서 고정합니다.

## 범위와 실행 위치

이 계획은 작업 A의 실행 계획입니다. 졸업요건의 다중전공 합격 시점(B), 영어/핵심교양 선택·대체·경과조치(C)는 연결된 설계 문서의 독립 후속 작업입니다. A의 구현으로 B/C가 완료되었다고 보고하지 않습니다.

저장소 루트:

```text
backend: /Users/sumin/dev/ahni/backend
mobile:  /Users/sumin/dev/ahni/frontend/mobile
admin:   /Users/sumin/dev/ahni/frontend/admin
```

각 저장소의 기존 변경을 먼저 확인하고 작업 단위별 `feat/yearly-curriculum` 브랜치를 사용합니다. 이미 같은 작업 브랜치가 있으면 재사용합니다. 문서 브랜치에서 프로덕션 구현을 시작하지 않습니다. 커밋은 사용자 요청과 각 저장소 git-workflow를 따릅니다.

## 계약과 파일 책임

백엔드 신규 파일은 `src/main/java/com/ahni/backend/` 하위입니다.

- `entity/Curriculum.java`, `entity/CurriculumCourse.java`, `domain/CurriculumDivision.java`, `domain/RecommendedTerm.java`: 출처·공개 상태·연도별 연결과 입력 불변식. DTO의 entity 의존을 금지하는 기존 계층 규칙을 유지합니다.
- `repository/CurriculumRepository.java`, `repository/CurriculumCourseRepository.java`: 공개 목록, 중복 조회, 행 잠금.
- `service/CurriculumService.java`: 관리자 권한 확인, 초안 저장, 공개, 학생 조회, 성적 연결 검증.
- `controller/CurriculumController.java`, `controller/AdminCurriculumController.java`: HTTP 입력/출력과 OpenAPI.
- `dto/CurriculumRequest.java`, `dto/CurriculumCourseRequest.java`, `dto/CurriculumResponse.java`, `dto/CurriculumCourseResponse.java`, `dto/CurriculumPublicationRequest.java`: 엔티티와 분리된 계약.
- `exception/CurriculumNotAvailableException.java`, `exception/CourseNotInCurriculumException.java`, `exception/CurriculumAlreadyExistsException.java`: 안정적인 실패 코드. 기존 전역 예외 처리기에 등록합니다.

관리자 요청 형태를 고정합니다. 이 payload는 교과과정 전체를 교체합니다. 빈 courses는 초안 저장에만 허용합니다.

```json
{
  "departmentEntityId": "00000000-0000-0000-0000-000000000001",
  "curriculumYear": 2024,
  "sourceTitle": "2024 소프트웨어융합공학과 교과과정표",
  "sourceUrl": null,
  "courses": [{
    "courseEntityId": "00000000-0000-0000-0000-000000000101",
    "division": "MAJOR_REQUIRED",
    "recommendedYear": 1,
    "recommendedTerm": "FIRST",
    "areaCode": null,
    "areaName": null,
    "majorArea": "컴퓨터기초",
    "note": null
  }]
}
```

관리자 응답: `entityId`, `version`, `department`, `curriculumYear`, `sourceTitle`, `sourceUrl`, `published`, `courses`. 수정 요청과 publication 요청은 조회한 version을 포함하며 충돌 시 409 CURRICULUM_EDIT_CONFLICT입니다. 생성 요청의 version은 null입니다. 연결 응답은 `entityId`, 기존 `CourseResponse`인 `course`, 요청의 분류·권장·영역·비고 필드입니다. 목록도 같은 응답 배열을 사용하며 학과명·연도순으로 정렬합니다. 숫자 FK는 노출하지 않습니다.

## Task 1: 데이터 보존형 스키마와 엔티티

**Files:** 위 entity/repository 파일을 생성합니다. `src/main/resources/db/migration/V18__create_yearly_curriculum.sql`, `src/test/java/com/ahni/backend/entity/CurriculumTest.java`, `src/test/java/com/ahni/backend/persistence/CurriculumMigrationIntegrationTest.java`를 생성합니다. 실행 시 V18이 이미 사용되었다면 기존 파일을 수정하지 말고 다음 번호를 사용합니다.

**Interfaces:** `Curriculum`은 department/year/source/published를 소유합니다. `CurriculumCourse`는 curriculum/course를 ManyToOne으로 참조합니다. 엔티티 생성과 변경은 길이, 범위, enum, 필수값을 검증합니다.

- [ ] RED: 기존 migration integration test의 Testcontainers/Flyway 설정을 재사용하고 아래 SQL 사례를 실행하는 테스트를 작성합니다. 각 사례는 별도 테스트 트랜잭션입니다.

```sql
-- 같은 학과·연도 두 번째 입력은 SQLSTATE 23505.
INSERT INTO curriculum (entity_id, department_id, curriculum_year,
 source_title, published, created_at, updated_at)
SELECT gen_random_uuid(), id, 2024, '공식 자료', false, now(), now()
FROM department ORDER BY id LIMIT 1;
-- 같은 INSERT를 다시 실행하여 UNIQUE 실패를 확인합니다.
-- 연결 생성 전후 기존 course/entity_id와 student_grade/course_id를 비교합니다.
SELECT entity_id FROM course ORDER BY id;
SELECT entity_id, course_id, credit FROM student_grade ORDER BY id;
```

- [ ] 실행: `./gradlew integrationTest --tests '*CurriculumMigrationIntegrationTest'`. 새 테이블 부재로 실패해야 합니다.
- [ ] GREEN: 설계 표의 컬럼을 생성합니다. UNIQUE 두 개, 연도/권장 학년 CHECK, division/term CHECK, 필수 컬럼 NOT NULL, FK NO ACTION을 적용합니다. SQL의 enum 허용값은 Java enum과 같아야 합니다.

```sql
CONSTRAINT uq_curriculum_department_year UNIQUE (department_id, curriculum_year),
CONSTRAINT ck_curriculum_year CHECK (curriculum_year BETWEEN 2000 AND 9999)
```

연결에 `UNIQUE(curriculum_id, course_id)`를 적용합니다. `@GeneratedValue(strategy = GenerationType.IDENTITY)`, UUID 외부 ID, 기존 시간 감사 규약을 재사용합니다. 연결을 삭제해도 course를 삭제하지 않도록 cascade REMOVE를 설정하지 않습니다.
- [ ] GREEN 확인: 같은 연결 중복 실패, 없는 FK 실패, enum 오류 실패, 자료 추가 후 기존 성적 보존을 검증합니다. 단위 테스트는 공백 출처/연도 1999/권장 학년 0을 거절하고 null 권장 정보를 허용합니다.
- [ ] 실행: `./gradlew test --tests '*CurriculumTest'`와 위 integrationTest. PASS 후 문서에 테스트 결과를 기록합니다.

## Task 2: 관리자 초안 저장·공개 API

**Files:** 위 service/controller/dto/exception 파일을 생성합니다. 기존 전역 예외 처리기와 `src/test/java/com/ahni/backend/controller/AdminCurriculumControllerTest.java`를 변경/생성합니다. 저장·공개 서비스의 트랜잭션은 별도 mock 서비스 테스트를 중복 작성하지 않고 이 HTTP/PostgreSQL 통합 테스트에서 검증합니다.

**Interfaces:** 서비스 public 메서드는 아래로 고정합니다. authority는 `AdminAccessService.getCurrentAdmin(authUserId)`를 사용합니다.

```java
List<CurriculumResponse> list(UUID authUserId);
CurriculumResponse get(UUID authUserId, UUID entityId);
CurriculumResponse create(UUID authUserId, CurriculumRequest request);
CurriculumResponse update(UUID authUserId, UUID entityId, CurriculumRequest request);
CurriculumResponse publish(UUID authUserId, UUID entityId, CurriculumPublicationRequest request);
```

- [ ] RED: 컨트롤러 테스트에서 기존 JWT/관리자 mock 패턴을 사용하여 무인증 401, 비관리자 403, 위 payload POST 201, 중복 학과·연도 409, 빈 초안 공개 400을 작성합니다.

```java
mockMvc.perform(get("/api/v1/admin/curricula"))
    .andExpect(status().isUnauthorized());
mockMvc.perform(get("/api/v1/admin/curricula").with(jwt()))
    .andExpect(status().isForbidden());
```

- [ ] 실행: `./gradlew integrationTest --tests '*AdminCurriculumControllerTest'`. 신규 서비스/경로 부재로 실패를 확인합니다.
- [ ] GREEN: `GET/POST /api/v1/admin/curricula`, `GET/PUT /api/v1/admin/curricula/{entityId}`, `PUT /api/v1/admin/curricula/{entityId}/publication`을 구현합니다. publication body는 `{"published":true,"version":0}`이며 version은 조회한 현재 값입니다. 수정 시 department/year는 기존과 동일해야 하며 변경 요청은 400 INVALID_REQUEST로 거절합니다. 다른 학과·연도 자료는 새로 생성합니다.
- [ ] GREEN: 전체 요청의 과목 존재·중복·enum/길이를 먼저 검증한 뒤 한 트랜잭션에서 연결을 교체합니다. 수정 성공 시 published=false, 실패 시 원본 공개 상태와 연결을 유지합니다. 빈 목록 공개는 INVALID_REQUEST로 거절합니다.
- [ ] 테스트 추가: 정상 과목 뒤에 없는 과목을 넣은 요청도 기존 전체 연결이 보존되는지 실제 DB에서 확인합니다. 학과 필수과목 연결은 해당 학과와 일치해야 하고 공통 교양은 department=null을 허용합니다.
- [ ] 위 테스트 PASS 후 각 API에 @Operation/@ApiResponse, 권한, payload, 오류 예시를 추가합니다. DB 유일성 충돌도 CURRICULUM_ALREADY_EXISTS로 매핑합니다.

## Task 3: 연도별 조회와 성적 저장 검증

**Files:** `CurriculumController`, `CurriculumService`, 두 repository, 기존 `GradeService`, `GradeController`, `GradeServiceTest`, `GradeControllerTest`, `AdminCurriculumControllerTest`, 신규 `service/CurriculumConcurrencyIntegrationTest`. 연도별 HTTP 조회·실제 성적 저장 사례는 관리자 통합 테스트에서 같은 공식 자료 fixture로 함께 검증합니다.

**Interfaces:** 서비스 내부 사용용 `Course requirePublishedCourse(int academicYear, UUID courseEntityId)`는 기존 성적 등록 트랜잭션에 참여합니다. 학생 조회용 `List<CourseResponse> getCourses(int academicYear, UUID departmentEntityId)`는 인증된 GET에서 호출합니다.

- [ ] RED: 테스트 DB에 2024 code=A, 2025 code=B인 동일 이름 과목을 서로 다른 공개 교과과정에 저장합니다. 2024 GET은 A만, 2025 GET은 B만 반환해야 합니다. 요청 연도가 없거나 범위가 잘못되면 400입니다.

```java
mockMvc.perform(get("/api/v1/curriculum-courses")
        .param("academicYear", "2024").with(jwt()))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.length()").value(1))
    .andExpect(jsonPath("$[0].code").value("A"));
```

- [ ] 실행: `./gradlew integrationTest --tests '*AdminCurriculumControllerTest'`와 `./gradlew test --tests '*GradeServiceTest'`. 신규 경로 부재와 연도별 검증 누락으로 실패하는지 확인합니다.
- [ ] GREEN: GET은 year 필수, department 선택 조건으로 공개 curriculum의 course를 DISTINCT, code ASC로 조회합니다. 존재하지 않는 학과는 기존 DEPARTMENT_NOT_FOUND, 공개 대상 없음은 CURRICULUM_NOT_AVAILABLE입니다. 학과 필터가 없으면 다른 학과에 공개된 같은 연도의 과목도 반환하며 필터가 있으면 그 학과의 연결만 반환합니다.
- [ ] GREEN: GradeService.register의 active-only 조회를 연도별 공개 연결 조회로 변경합니다. 교과과정 없음은 404, 공개 연도는 있지만 과목이 연결되지 않은 경우는 400 COURSE_NOT_IN_CURRICULUM입니다. 초안만 있는 연도도 404입니다. 과거 비활성 과목은 명시적인 공개 연결이 있을 때만 허용합니다.

```java
Course course = curriculumService.requirePublishedCourse(
    request.academicYear(), request.courseEntityId());
// update에서는 기존 수강 연도와 다른 경우에만 같은 검증을 호출합니다.
```

- [ ] GREEN: 같은 연도의 curriculum을 ID순 PESSIMISTIC_READ로 잠근 뒤 공개 상태와 연결을 확인합니다. 관리자 update/publish는 PESSIMISTIC_WRITE를 사용하고 version 불일치를 거절합니다. GradeService의 @Transactional 안에서 저장까지 잠금을 유지합니다. DTO 변환과 공개 검색은 fetch join으로 N+1을 방지합니다.
- [ ] 테스트 추가: 2024에 B 등록 거절, 비활성 A 등록 성공, RPL에서도 다른 연도 과목 거절, 비소유자 수정 거절, 같은 연도의 기존 성적 등급 수정 성공, 연도 변경 불일치 거절. 등록/수정 실패 시 기존 성적·재수강 연결이 바뀌지 않아야 합니다.
- [ ] 통합 테스트: 독립 트랜잭션 두 개와 CountDownLatch로 관리자 비공개 변경을 먼저 커밋한 뒤 대기하던 등록이 404인 경우를 만듭니다. 반대 순서에서는 등록 커밋까지 관리자 변경이 기다리는지 확인합니다. sleep으로 타이밍을 추측하지 않고 timeout이 있는 await를 사용합니다.
- [ ] 실행: `./gradlew integrationTest --tests '*CurriculumConcurrencyIntegrationTest'`와 위 단위 테스트. PASS 후 GradeController의 400/404 예시를 갱신합니다.

## Task 4: 모바일 연도 우선 선택

**Files:** 기존 `lib/features/grade/data/course_api.dart`, `application/course_catalog_controller.dart`, `presentation/grade_registration_page.dart`, `presentation/grade_edit_page.dart`. 해당 `test/unit/features/grade/` API/controller 테스트와 `test/widget/grade/` 등록/수정 테스트를 변경합니다.

**Interfaces:** 기존 구조를 유지하며 파라미터만 확장합니다.

```dart
Future<List<CourseCatalogItem>> getCourses(
  String accessToken, {required int academicYear});
Future<void> load({required int academicYear, bool force = false});
```

- [ ] RED: MockClient로 GET 경로와 query를 확인합니다. CURRICULUM_NOT_AVAILABLE은 전용 안내, 401은 기존 로그인 만료 처리를 확인합니다.

```dart
expect(request.url.path, '/api/v1/curriculum-courses');
expect(request.url.queryParameters['academicYear'], '2024');
```

- [ ] 실행: `flutter test test/unit/features/grade/data/course_api_test.dart`. 변경한 서명/요청 때문에 실패해야 합니다.
- [ ] GREEN: Uri.replace(queryParameters)를 사용하고 404 body의 code를 구분합니다. 예외 메시지는 '이 연도의 교과과정이 아직 등록되지 않았어요.'입니다. 알 수 없는 오류와 잘못된 JSON은 기존 recoverable 오류를 유지합니다.
- [ ] RED: 두 Completer로 2024/2025 요청을 만들고 2025 성공 후 2024를 성공 또는 실패시킵니다. 최종 state에는 2025 과목만 남아야 합니다. reset/dispose 후 응답에도 notify하지 않는 테스트를 추가합니다.
- [ ] GREEN: 매 load 시작 시 generation을 증가시키고 목록을 비웁니다. 같은 연도 로딩 중 중복 요청만 생략하며 다른 연도 요청은 시작합니다. retry는 마지막 연도를 사용하고 reset은 연도·검색·목록을 제거합니다. dispose도 세대를 무효화합니다.
- [ ] RED/GREEN: widget 테스트에서 수강 연도를 변경하면 선택 과목, 자동 입력 학점, 검색, 재수강 후보가 초기화되는지 확인합니다. 연도는 2000~현재 연도만 허용하며 미완성 값/로딩 중 선택·저장을 차단합니다. 필요 시 year 입력의 onChanged에서 기존 값을 초기화하고 유효한 정수일 때만 load합니다.
- [ ] 수정 화면은 기존 과목을 이력으로 표시합니다. 연도 변경 저장이 COURSE_NOT_IN_CURRICULUM이면 과목을 임의 변경하지 않고 '이 연도에는 해당 과목을 선택할 수 없어요. 수강 연도를 확인하거나 성적을 새로 등록해 주세요.'로 안내합니다. 동일 연도 등급 수정은 서버 성공 결과를 유지합니다. GradeApi의 기존 오류 파싱을 재사용하고 새 코드 테스트를 추가합니다.
- [ ] 실행: `flutter test test/unit/features/grade test/widget/grade` 및 `./scripts/verify`. 기기 테스트는 사용자에게 연도 변경·이전 과목 제외·재시도 사례를 전달합니다.

## Task 5: 관리자 교과과정 편집·공개 화면

**Files:** 신규 `src/features/curricula/CurriculumManagementPage.tsx`, `CurriculumEditor.tsx`, `curriculumForm.ts`, `e2e/curricula.spec.ts`; 기존 `src/shared/api/contracts.ts`, `contracts.test.ts`, `backendApi.ts`, `src/app/App.tsx`, `App.test.tsx`, `AdminShell.tsx`. 컴포넌트 상호작용은 기존 App.test.tsx, 브라우저 흐름은 e2e에서 검증합니다.

**Interfaces:** BackendApi에 `listCurricula()`, `createCurriculum(request)`, `updateCurriculum(entityId, request)`, `setCurriculumPublication(entityId, published, version)`을 추가합니다. 응답은 Zod schema 검증 후 사용합니다. 후보는 기존 listAdminCourses/listDepartments를 재사용합니다.

- [ ] RED: `/curricula` 무권한 접근은 로그인으로 이동하고, 관리자 접근은 학과/연도/출처/공개 상태를 보여주는 테스트를 작성합니다. API fake는 새 메서드를 반드시 구현합니다.
- [ ] 실행: `pnpm test src/app/App.test.tsx src/shared/api/contracts.test.ts`. 새 route/schema가 없어서 실패를 확인합니다.
- [ ] GREEN: 선택한 자료의 courses만 표시하고 전체 목록에서는 기존 과목명·학수번호 검색으로 연결을 추가합니다. division/권장 학년·학기/영역/비고를 편집할 수 있게 합니다. 학과·연도 수정은 불가, 새 자료 생성은 허용합니다. 공개 버튼은 저장 완료된 자료에만 사용합니다.

```ts
const curriculumPublicationSchema = z.object({
  published: z.boolean(), version: z.number().int().nonnegative().safe(),
})
const publication = curriculumPublicationSchema.parse({ published: true, version: 0 })
// backendApi에서는 기존 인증/401/403 처리와 Zod 응답 검증을 재사용합니다.
```

- [ ] RED/GREEN: 저장 실패 시 입력 유지, 중복 과목 차단, 초안 저장 후 공개 상태 false 표시, 빈 자료 공개 차단, 403 로그인 이동, 빠른 자료 선택 변경 중 이전 응답 무시를 interaction 테스트로 검증합니다. 기존 PageHeader/Button/StatusMessage와 폼 스타일을 사용합니다.
- [ ] 공개 확인에는 '자료의 과목 목록과 출처를 확인했습니다.' 체크를 요구합니다. 누락된 교양을 자동 생성하지 않습니다. 비활성 과목 연결은 과거 공식 자료 확인 안내를 보여줍니다.
- [ ] 실행: `pnpm test src/features/curricula src/app src/shared/api`와 `./scripts/verify`. 실제 UI 확인은 격리된 테스트 자료로 수행하며 공유 DB에 테스트 행을 남기지 않습니다.

## Task 6: 계약·운영 적용 문서와 전체 회귀

**Files:** backend `docs/api/openapi.json`, `docs/api/README.md`, `docs/domain/index.md`, 신규 `docs/development/yearly-curriculum.md`; mobile/admin `contracts/backend-openapi.json`과 각 API 계약 문서.

- [ ] 백엔드 기존 OpenAPI 생성 절차로 JSON을 갱신합니다. 200/201/400/401/403/404/409, academicYear 필수, 배열 응답, 요청 enum을 계약 테스트에 고정합니다. 계약 JSON을 손으로 수정하지 않습니다.
- [ ] 양쪽 클라이언트에서 실행합니다.

```bash
./scripts/update-api-contract /Users/sumin/dev/ahni/backend/docs/api/openapi.json
```

- [ ] dev는 Flyway 비활성·ddl-auto=update이므로 생성된 표만 보고 migration 적용을 완료로 간주하지 않습니다. 운영 문서에 백업 → Flyway 적용 DB와 dev 차이 확인 → V18의 새 표·제약 및 승인된 V19 RLS 적용 → 학과/연도별 초안 등록 → 과목 코드/149개 연결 대조 → 자료 완비 확인 후 공개 → 클라이언트 배포 순서를 남깁니다. 기존 migration 전체 재실행·DROP·volume 삭제는 사용하지 않습니다.
- [ ] 신규 catalog API만 준비한 상태에서는 기존 클라이언트가 동작하도록 배포하고, 자료 공개와 새 클라이언트 준비 후 성적 등록 서버 검증을 활성화하는 배포 순서를 잡습니다. 별도 우회 flag는 추가하지 않습니다. 필요하면 Task 3의 조회와 성적 저장 변경을 별도 커밋으로 분리합니다.
- [ ] 세 저장소에서 `./scripts/verify`를 실행하고 결과를 기록합니다. `git diff --check`와 변경 파일 목록을 확인합니다. production 데이터 입력 없이 unit/integration/contract 테스트가 모두 통과해야 코드 완료입니다.
- [ ] 사용자 기기 확인: 2024 선택 시 2025 전용 코드 미노출, 연도 변경 초기화, 자료 없는 연도 안내, 기존 성적 수정, RPL/재수강 회귀. 공식 교양 추가 자료가 없으면 해당 연도를 완비했다고 보고하지 않습니다.

## 후속 작업 인계

실제 수행 상태·검증 결과·운영 적용 보류 항목은 [실행 기록](2026-10-06-yearly-curriculum-progress.md)에 남깁니다. 위 체크리스트는 실행 절차입니다. 2026-10-07 후속 승인으로 두 DB의 스키마·RLS 적용은 완료했으며 공식 자료 입력과 사용자 기기 확인은 별도입니다.

작업 B는 전공 합격 시점과 기준 연도 분리, 작업 C는 영어 1-of-3·핵심교양 A/B 및 공식 대체 관계입니다. 상세 파일/인터페이스는 A의 모델·계약이 확정된 뒤 각 작업 시작 전에 독립 계획으로 고정합니다. 현재의 평면 required_course만으로 영어 3과목을 모두 필수 지정하거나 핵심교양 A/B를 혼합 판정하지 않습니다.
