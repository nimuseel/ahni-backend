# Git workflow

## 기본 원칙

- `main`은 항상 배포 가능한 상태로 유지하며, 일반 작업은 기능 브랜치에서 진행합니다.
- 브랜치 이름은 작업 성격에 따라 `<type>/<짧은-작업명>` 형식을 사용합니다. 예: `feat/student-auth`.
- 하나의 커밋은 하나의 논리적 변경만 담습니다. 포맷팅, 생성물, 무관한 리팩터링을 섞지 않습니다.
- 비밀번호, 토큰, 개인 키, 로컬 설정 파일과 빌드 산출물은 커밋하지 않습니다.

## 커밋 전 절차

1. `git status --short`로 변경 범위를 확인합니다.
2. `git diff`로 의도하지 않은 변경과 민감정보를 확인합니다.
3. `./scripts/verify`를 실행해 테스트와 정적 검사를 통과시킵니다.
4. 관련 파일만 경로를 지정해 스테이징합니다.
5. `git diff --cached --check`와 `git diff --cached --stat`으로 staged diff를 확인합니다.

## 커밋 메시지

Conventional Commits 형식을 사용합니다.

```text
<type>(<scope>): <imperative summary>
```

허용 type은 `feat`, `fix`, `docs`, `refactor`, `chore`입니다. 브랜치 type과 커밋 type은 작업 성격에 맞춰 동일하게 사용합니다. 제목은 짧고 명령형으로 작성하며, 필요할 때 본문에 변경 이유와 영향 범위를 적습니다.

| type | 용도 |
| --- | --- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 작성 및 수정 |
| `refactor` | 동작 변경 없는 구조 개선 |
| `chore` | 설정, 의존성, 빌드 등 유지보수 |

## 푸시와 리뷰

- 커밋 후 `git log -1 --oneline`으로 커밋을 확인한 뒤 `git push -u origin <branch>`로 현재 브랜치를 푸시합니다.
- `main`에 직접 푸시하지 않고 Pull Request를 통해 병합합니다.
- 강제 푸시(`--force`, `--force-with-lease`)는 사용하지 않습니다.
- PR에는 변경 목적, 검증 명령과 결과, 데이터베이스·API·보안 영향 및 후속 작업을 기록합니다.
- Copilot 등 AI 도구가 만든 변경도 작성자가 직접 diff와 테스트 결과를 확인한 뒤 커밋합니다. 사용자 요청 없이 자동 커밋하거나 푸시하지 않습니다.
