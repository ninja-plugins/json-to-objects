# /harness-apply

## 목적

현재 저장소 또는 대상 저장소에 ninja-harness를 적용하거나 적용 상태를 보정한다.

## 절차

1. `.claude/skills/apply-harness/SKILL.md`를 읽고 신규 적용, 다운스트림 업그레이드, 준비도 보정 중 하나로 분류한다.
2. 현재 저장소 작업이면 `docs/harness/context/BASELINE.md`, `docs/harness/context/INDEX.md`, `docs/harness/README.md`, `docs/harness/QUICKSTART_5_MIN.md`를 확인한다.
3. 다른 저장소가 대상이면 먼저 `make apply-harness TARGET=<target-repo>`로 dry-run을 실행한다.
4. 사용자가 적용을 요청했거나 dry-run 이후 승인이 있으면 `make apply-harness TARGET=<target-repo> APPLY=1` 또는 `scripts/apply-harness-to-project.* --apply`를 사용한다.
5. 기존 `AGENTS.md`, `CLAUDE.md`, `Makefile`, `.github/**`, `.codex/**`, `.claude/**`는 덮어쓰기 전에 병합 여부를 확인한다.
6. 적용 후 `make doctor`, `make verify`, `make project-ready`를 실행하고 실패하면 profile/context 또는 구조 누락을 보정한다.
7. 실제 프로젝트 build/test/lint가 필요하면 `HARNESS_*_SCRIPT` project gate를 연결한 뒤 검증한다.

