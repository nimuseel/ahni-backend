# 평점 시뮬레이션

`POST /api/v1/grades/simulation`은 JWT의 학생에게 등록된 성적과 예상 성적을 합산합니다. 조회용 계산이며 예상 성적, 요약, 재수강 관계를 DB에 저장하거나 수정하지 않습니다. 기존 API와 호환되는 추가 엔드포인트이며 모바일이 소비합니다.

## 요청

`Authorization: Bearer <access-token>`과 `Content-Type: application/json`이 필요합니다. 학생 ID, 과목 ID, 연도, 학기를 받지 않습니다.

```json
{
  "expectedGrades": [
    {"category": "MAJOR", "credit": 3.0, "gradeCode": "A_PLUS"}
  ]
}
```

- `expectedGrades`: 1~50건. null 항목은 허용하지 않습니다.
- `category`: `MAJOR`, `GENERAL_EDUCATION`, `ELECTIVE`.
- `credit`: 0 초과 30 이하, 소수점 한 자리.
- `gradeCode`: 기존 `A_PLUS`~`F`, `P`, `NP` 등급. 필수입니다.

## 계산과 응답

`200` 응답의 `current`와 `projected`는 각각 `GradeSummaryResponse`입니다. 두 요약 모두 전체 GPA, 이수학점, GPA 반영 학점, 세 분류의 요약을 포함합니다. 전체 성공 응답 예시는 [OpenAPI 계약](openapi.json)의 해당 엔드포인트에 포함되어 있습니다.

현재 이력이 3학점 B0이고 3학점 A+를 예상하면 현재 GPA는 3.00, 예상 GPA는 3.75입니다. 현재 성적이 없어도 예상 성적만 계산할 수 있으며 현재 요약은 0입니다.

실제 성적의 재수강 대체 관계와 학점·평점 스냅샷을 유지합니다. RPL/P는 이수학점에 포함하지만 GPA에서 제외하고, NP는 둘 모두 제외합니다. F는 이수학점에 포함하지 않고 GPA 분모에 포함합니다. GPA는 4.5점 기준 학점 가중 평균을 소수점 두 자리 HALF_UP으로 반환합니다. 반올림된 현재 GPA를 다시 곱하지 않고 원래의 가중합에서 계산합니다.

첫 범위는 예상 성적 추가뿐입니다. 예상 재수강, 예상 RPL, 목표 GPA 역산, 공식 학교 평점 인증은 지원하지 않습니다.

## 오류

| 상태 | 코드 | 의미 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | 목록 개수, null, 학점, 분류 또는 등급 오류 |
| 401 | 없음 | 누락되거나 유효하지 않은 Bearer 인증 |
| 404 | `STUDENT_NOT_FOUND` | JWT 사용자에게 학생 프로필이 없음 |

```json
{"code":"INVALID_REQUEST","message":"요청값이 올바르지 않습니다."}
```

```json
{"code":"STUDENT_NOT_FOUND","message":"학생 프로필을 찾을 수 없습니다."}
```

## 검증

```bash
./gradlew test --tests '*GradeSummaryCalculatorTest' --tests '*GradeServiceTest'
./gradlew integrationTest --tests '*GradeSimulationIntegrationTest' --tests '*OpenApiContractTest'
./scripts/verify
```

HTTP 통합 테스트는 격리된 테스트 DB에서 다른 학생의 성적 제외와 호출 전후 성적 값·기록 시간 보존을 확인합니다. 개발 DB나 공유 Supabase 데이터는 사용하지 않습니다.
