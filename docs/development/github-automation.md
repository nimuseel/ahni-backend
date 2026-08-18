# GitHub 자동화

## 동작

- `feat/**`, `fix/**`, `docs/**`, `refactor/**`, `chore/**` 브랜치에 푸시하면 `auto-pr.yml`이 `main` 대상 PR을 자동 생성합니다.
- 동일한 브랜치에 열린 PR이 있으면 새 PR을 만들지 않습니다.
- PR은 `GH_PAT` Secret의 사용자 계정으로 생성되어 Copilot 사용 권한이 있는 작성자로 인식됩니다.
- PR 생성 후 GitHub CLI의 `@copilot` 리뷰 요청을 실행합니다.
- PR과 대상 브랜치에 `ci.yml`이 실행되며 `./scripts/verify`가 통과해야 합니다.

## GitHub에서 한 번만 설정할 항목

### 1. Actions Secret

저장소 `Settings → Secrets and variables → Actions`에 `GH_PAT` Secret을 추가합니다.

Fine-grained Personal Access Token을 권장하며, 대상 저장소에 대해 다음 권한을 부여합니다.

- Contents: Read-only
- Pull requests: Read and write
- Metadata: Read-only

토큰은 PR 생성 주체를 사용자 계정으로 만들기 위한 용도로만 사용하며, 워크플로 로그에 출력하지 않습니다.

### 2. Copilot 자동 리뷰

저장소 Settings의 Copilot automatic code review에서 `main`을 대상 브랜치로 추가하고 다음을 활성화합니다.

- Automatically request Copilot code review
- Review new pushes
- 필요하면 Review draft pull requests

Copilot 리뷰는 의견(Comment) 리뷰이며 사람의 승인이나 병합 차단을 대체하지 않습니다. Copilot 요금제, AI credits, GitHub Actions 사용량 및 저장소 권한이 필요합니다.
