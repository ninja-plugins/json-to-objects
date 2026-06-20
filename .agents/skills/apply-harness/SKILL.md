---
name: apply-harness
description: use when applying ninja-harness to another project or downstream repository, upgrading an applied harness, filling project profiles, wiring project gates, or validating project-ready readiness.
---

# 하네스 적용

공통 운영 기준: `docs/harness/SKILL_AUTHORING.md#공통-운영-블록`

## 먼저 분류

- 신규 적용: 대상 저장소에 하네스 파일을 복사하고 프로젝트별 profile/context 값을 채운다.
- 다운스트림 업그레이드: 기존 적용 저장소의 프로젝트 소유 context/profile/plan을 보존하고 managed file만 병합한다.
- 준비도 보정: `BASELINE.md`, `profiles/**`, `harness.yaml` placeholder와 project gate 연결만 정리한다.

## 먼저 읽을 문서

- `AGENTS.md`
- `docs/harness/README.md`
- `docs/harness/QUICKSTART_5_MIN.md`
- `docs/harness/CONFIGURATION.md`
- 업그레이드이면 `docs/harness/UPGRADE.md`
- 대상 저장소에 이미 있으면 `AGENTS.md`, `CLAUDE.md`, `Makefile`, CI 설정, package/build 설정

## 핵심 기준

- 대상 repo 경로가 있으면 먼저 `python3 scripts/apply-harness-to-project.py --target <target-repo>` 또는 `make apply-harness TARGET=<target-repo>`로 dry-run을 실행한다.
- 사용자가 적용까지 요청했거나 dry-run 결과를 확인한 뒤에는 `--apply` 또는 `APPLY=1`로 파일을 쓴다.
- profile/context만 다시 생성할 때는 `--profile-only` 또는 `PROFILE_ONLY=1`을 사용한다.
- 기존 대상 저장소의 `AGENTS.md`, `CLAUDE.md`, `Makefile`, `.github/**`, `.codex/**`, `.claude/**`는 읽고 병합한다.
- 공통 하네스 기준은 `docs/harness/**`, scripts, agents, skills에 두고 프로젝트별 값은 `context/**`, `profiles/**`, `harness.yaml`, 루트 agent 지침에 둔다.
- 사용하지 않는 영역은 `<...>` placeholder로 남기지 않고 `N/A`로 채운다.
- 적용 후 `make doctor`, `make verify`, `make project-ready`를 실행한다.
- 실제 build/test/lint가 있으면 `HARNESS_*_SCRIPT`로 연결하고 `make verify-org` 또는 project gate를 실행한다.
- 스킬을 바꿨으면 `make sync-skills` 후 mirror drift를 검증한다.

## 복사/병합 체크포인트

- 복사 후보: `docs/harness/**`, `.agents/**`, `.codex/**`, `.claude/**`, `scripts/**`, `VERSION`
- 병합 후보: `AGENTS.md`, `CLAUDE.md`, `Makefile`, `.github/**`, `.gitignore`, `LICENSE`
- 다운스트림 업그레이드에서는 `docs/harness/context/**`, `docs/harness/profiles/**`, `docs/harness/plans/**`를 템플릿 값으로 덮어쓰지 않는다.
