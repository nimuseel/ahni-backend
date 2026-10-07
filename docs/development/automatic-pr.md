# 작업자 계정으로 자동 PR 생성

작업자는 커밋의 author 이름이 아니라 push 이벤트의 GitHub 사용자(`github.actor`)입니다.
워크플로 재실행 시에도 최초 push 사용자를 기준으로 하며, 재실행한 사람의 계정으로 바꾸지 않습니다.

## 개인 PAT 등록

각 사용자가 **자신의 GitHub 계정**에서 PAT를 발급하고 접근 가능한 대상 저장소에만 등록합니다.
다른 사람의 토큰을 사용자 이름만 바꾸어 등록하지 않습니다.

각 저장소의 **Settings → Secrets and variables → Actions → New repository secret**에서 등록합니다.

| GitHub 사용자 | Secret 이름        |
| ------------- | ------------------ |
| nimuseel      | GH_PAT_NIMUSEEL    |
| team-member   | GH_PAT_TEAM_MEMBER |

이름은 `GH_PAT_` 뒤에 GitHub 사용자 ID를 대문자로 붙이고, 하이픈은 밑줄로 바꿉니다.
Secret 값은 그 사용자의 PAT이며 코드, .env, PR 본문이나 로그에 기록하지 않습니다.

- 현재 공용 `GH_PAT`는 더 이상 사용하지 않습니다. nimuseel 계정도 `GH_PAT_NIMUSEEL`을 별도로 등록해야 합니다.
- 세 저장소에서 모두 자동 생성하려면 각 저장소에 해당 사용자의 Secret을 등록해야 합니다.
- 가능한 경우 fine-grained PAT로 대상 저장소만 선택하고 Contents read, Pull requests read/write 권한과 만료일을 설정합니다.
- 개인 계정 소유 저장소의 외부 협업자 등 fine-grained PAT가 지원하지 않는 경우가 있습니다. 해당 사용자의 접근 가능 여부를 먼저 확인합니다. classic PAT가 필요하면 공개 저장소에는 public_repo, 비공개에는 repo 범위가 필요하며 범위가 더 넓다는 점을 고려해야 합니다.
- 등록 권한이 없는 팀원은 저장소 관리자에게 안전한 등록 절차를 요청합니다. 채팅이나 저장소 파일로 토큰을 공유하지 않습니다.
- Actions 워크플로를 수정할 수 있는 협업자는 Secret을 사용하는 코드를 작성할 수 있습니다. 신뢰할 수 있는 협업자만 쓰기 권한을 갖도록 하고, 퇴사·탈퇴·만료 시 토큰을 폐기하거나 갱신합니다.

등록 방법: [GitHub Actions Secrets](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets)
권한과 제한: [GitHub PAT 관리](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)

## 실행 정책

1. 기존 feat/fix/docs/refactor/chore 브랜치 push를 감지합니다.
2. push 사용자에 해당하는 Secret만 선택합니다. 공용 PAT나 봇 토큰으로 대체하지 않습니다.
3. Secret이 없으면 등록할 이름을 안내하고 실패합니다. 토큰 값은 출력하지 않습니다.
4. GitHub의 인증 사용자 조회로 PAT 소유자와 push 사용자의 ID를 대소문자 구분 없이 비교합니다.
5. 불일치·인증 실패 시 PR 조회·생성·갱신과 Copilot 리뷰 요청 전에 중단합니다.
6. 일치하면 PR 생성 또는 기존 PR의 커밋 내역 갱신 후 같은 PAT로 Copilot 리뷰를 요청합니다.

기존 PR은 작성자를 바꿀 수 없으므로 삭제·재생성하지 않습니다. 다른 팀원이 같은 브랜치에 push해도 기존 PR 작성자는 유지됩니다.
새 정책은 새로 생성되는 PR의 작성자에 적용됩니다. 각 사용자가 자신의 작업 브랜치를 사용하는 것이 기준입니다.
Copilot 사용 권한과 저장소 리뷰 정책은 별도로 충족해야 합니다.

## 검증과 적용

Node.js 24.19.0에서 `node --test .github/tests/auto-pr.test.cjs`를 실행합니다.
이 테스트는 실제 워크플로의 JavaScript와 Bash를 실행하며 GitHub 네트워크 응답만 테스트용으로 대체합니다.
`./scripts/verify`와 CI에도 포함됩니다.

브랜치의 변경을 커밋·push하면 해당 브랜치의 새 워크플로가 실행됩니다.
병합 전에도 push한 사용자의 새 Secret이 필요합니다. main 병합 후 다른 작업 브랜치에도 최신 워크플로를 반영합니다.
등록 후 실제 push에서 Actions 성공, PR 작성자와 Copilot 리뷰 요청을 확인합니다. 로컬 테스트만으로 실제 토큰 권한·계정·리뷰 실행 성공을 보장하지 않습니다.
