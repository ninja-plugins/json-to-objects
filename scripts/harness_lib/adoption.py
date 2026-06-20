from __future__ import annotations

import argparse
from dataclasses import dataclass, field
import json
import re
import shutil
import sys
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parent.parent.parent

MANAGED_DIRS = (
    'docs/harness',
    '.agents',
    '.codex',
    '.claude',
    '.github',
    'scripts',
    'tests',
)

ROOT_COPY_IF_MISSING = (
    'AGENTS.md',
    'CLAUDE.md',
    'Makefile',
    'README.md',
    'LICENSE',
    '.gitignore',
    '.github/CODEOWNERS',
)

ROOT_ALWAYS_COPY = (
    'VERSION',
)

PROJECT_OWNED_PREFIXES = (
    Path('docs/harness/context'),
    Path('docs/harness/profiles'),
    Path('docs/harness/plans'),
)

IGNORED_PARTS = {
    '.git',
    '.idea',
    '.pytest_cache',
    '__pycache__',
    'node_modules',
    'build',
    'dist',
    'target',
    '.next',
}

IGNORED_SUFFIXES = {
    '.pyc',
    '.pyo',
}

PROFILE_PATHS = (
    Path('MANIFEST.md'),
    Path('docs/harness/context/BASELINE.md'),
    Path('docs/harness/profiles/project-profile.md'),
    Path('docs/harness/profiles/design-system-profile.md'),
    Path('docs/harness/harness.yaml'),
)

PLACEHOLDER = re.compile(r'<[^>\n]+>')


@dataclass
class Area:
    key: str
    label: str
    path: str
    stack: str
    package_tool: str
    test_command: str
    build_command: str
    style_command: str = 'N/A'
    runtime_command: str = 'N/A'


@dataclass
class ProjectScan:
    root: Path
    project_name: str
    backend: Area
    primary_frontend: Area
    secondary_app: Area
    detected_files: list[str] = field(default_factory=list)
    notes: list[str] = field(default_factory=list)


@dataclass
class AdoptionResult:
    source: Path
    target: Path
    apply: bool
    profile_only: bool
    scan: ProjectScan
    planned: list[str] = field(default_factory=list)
    written: list[str] = field(default_factory=list)
    skipped: list[str] = field(default_factory=list)
    conflicts: list[str] = field(default_factory=list)
    notes: list[str] = field(default_factory=list)

    def to_dict(self) -> dict[str, object]:
        return {
            'source': str(self.source),
            'target': str(self.target),
            'apply': self.apply,
            'profile_only': self.profile_only,
            'scan': scan_to_dict(self.scan),
            'planned': self.planned,
            'written': self.written,
            'skipped': self.skipped,
            'conflicts': self.conflicts,
            'notes': self.notes,
        }


def rel_label(path: Path, root: Path) -> str:
    try:
        return path.relative_to(root).as_posix()
    except ValueError:
        return path.as_posix()


def should_ignore(path: Path) -> bool:
    if any(part in IGNORED_PARTS for part in path.parts):
        return True
    if (
        len(path.parts) >= 4
        and path.parts[:3] == ('docs', 'harness', 'plans')
        and path.parts[3] in {'active', 'completed'}
        and path.suffix == '.md'
    ):
        return True
    if path.suffix in IGNORED_SUFFIXES:
        return True
    if path.name in {'.DS_Store'} or path.name.startswith('._'):
        return True
    if path.name.startswith('__tmp-') and path.suffix == '.sh':
        return True
    return False


def is_project_owned(rel: Path) -> bool:
    return any(rel == prefix or prefix in rel.parents for prefix in PROJECT_OWNED_PREFIXES)


def has_placeholders(path: Path) -> bool:
    if not path.exists() or not path.is_file():
        return True
    return PLACEHOLDER.search(path.read_text(encoding='utf-8')) is not None


def read_package_json(path: Path) -> str:
    package_json = path / 'package.json'
    if not package_json.exists():
        return ''
    try:
        return package_json.read_text(encoding='utf-8').lower()
    except UnicodeDecodeError:
        return ''


def command_prefix_for_package_tool(tool: str) -> str:
    if tool == 'pnpm':
        return 'pnpm'
    if tool == 'yarn':
        return 'yarn'
    if tool == 'npm':
        return 'npm run'
    return 'N/A'


def package_tool_for(path: Path) -> str:
    if (path / 'pnpm-lock.yaml').exists():
        return 'pnpm'
    if (path / 'yarn.lock').exists():
        return 'yarn'
    if (path / 'package-lock.json').exists() or (path / 'package.json').exists():
        return 'npm'
    if (path / 'pom.xml').exists():
        return 'maven'
    if (path / 'gradlew').exists() or (path / 'build.gradle').exists() or (path / 'build.gradle.kts').exists():
        return 'gradle'
    if (path / 'pyproject.toml').exists():
        return 'python'
    return 'N/A'


def commands_for(path: Path, stack: str, tool: str) -> tuple[str, str, str, str]:
    if tool in {'npm', 'pnpm', 'yarn'}:
        prefix = command_prefix_for_package_tool(tool)
        package_text = read_package_json(path)
        test = f'{prefix} test' if '"test"' in package_text else 'N/A'
        build = f'{prefix} build' if '"build"' in package_text else 'N/A'
        style = f'{prefix} lint' if '"lint"' in package_text else 'N/A'
        runtime = f'{prefix} dev' if '"dev"' in package_text else 'N/A'
        return test, build, style, runtime
    if tool == 'maven':
        return 'mvn test', 'mvn package', 'N/A', 'mvn spring-boot:run' if 'spring' in stack.lower() else 'N/A'
    if tool == 'gradle':
        gradle = './gradlew' if (path / 'gradlew').exists() else 'gradle'
        return f'{gradle} test', f'{gradle} build', 'N/A', f'{gradle} bootRun' if 'spring' in stack.lower() else 'N/A'
    if tool == 'python':
        return 'python -m pytest', 'N/A', 'N/A', 'N/A'
    return 'N/A', 'N/A', 'N/A', 'N/A'


def stack_for(path: Path) -> str:
    package_text = read_package_json(path)
    if package_text:
        if '@nestjs' in package_text:
            return 'Node.js NestJS'
        if 'next' in package_text:
            return 'React Next.js'
        if 'vite' in package_text and 'vue' in package_text:
            return 'Vue Vite'
        if 'vite' in package_text and 'react' in package_text:
            return 'React Vite'
        if 'react-native' in package_text or 'expo' in package_text:
            return 'React Native / Expo'
        if 'vue' in package_text:
            return 'Vue'
        if 'react' in package_text:
            return 'React'
        return 'Node.js'
    if (path / 'pom.xml').exists() or (path / 'build.gradle').exists() or (path / 'build.gradle.kts').exists():
        build_text = ''
        for candidate in ('pom.xml', 'build.gradle', 'build.gradle.kts'):
            file_path = path / candidate
            if file_path.exists():
                build_text += file_path.read_text(encoding='utf-8', errors='ignore').lower()
        return 'Spring Boot' if 'spring' in build_text else 'JVM'
    if (path / 'pyproject.toml').exists():
        return 'Python'
    return 'N/A'


def score_backend(path: Path) -> int:
    name = path.name.lower()
    score = 0
    if name in {'backend', 'api', 'server', 'service'}:
        score += 5
    if any((path / item).exists() for item in ('pom.xml', 'build.gradle', 'build.gradle.kts', 'pyproject.toml')):
        score += 4
    package_text = read_package_json(path)
    if '@nestjs' in package_text or 'express' in package_text or 'fastify' in package_text:
        score += 4
    if (path / 'src' / 'main').exists():
        score += 2
    return score


def score_frontend(path: Path) -> int:
    name = path.name.lower()
    score = 0
    if name in {'frontend', 'front', 'web', 'client', 'admin', 'employee', 'employer'}:
        score += 5
    package_text = read_package_json(path)
    if any(token in package_text for token in ('react', 'vue', 'next', 'vite')):
        score += 5
    if (path / 'src').exists() and (path / 'package.json').exists():
        score += 1
    return score


def score_secondary(path: Path) -> int:
    name = path.name.lower()
    score = 0
    if name in {'app', 'mobile', 'secondary-app', 'native'}:
        score += 5
    package_text = read_package_json(path)
    if 'react-native' in package_text or 'expo' in package_text:
        score += 6
    if (path / 'android').exists() or (path / 'ios').exists():
        score += 2
    return score


def candidate_dirs(root: Path) -> list[Path]:
    candidates = [root]
    for child in sorted(root.iterdir(), key=lambda item: item.name):
        if child.is_dir() and not should_ignore(child):
            candidates.append(child)
    return candidates


def best_area(root: Path, key: str, label: str, scorer) -> Area:
    scored = sorted(((scorer(path), path) for path in candidate_dirs(root)), key=lambda item: (-item[0], item[1].as_posix()))
    score, path = scored[0] if scored else (0, root)
    if score <= 0:
        return Area(key, label, 'N/A', 'N/A', 'N/A', 'N/A', 'N/A')
    rel = '.' if path == root else path.relative_to(root).as_posix()
    stack = stack_for(path)
    tool = package_tool_for(path)
    test, build, style, runtime = commands_for(path, stack, tool)
    return Area(key, label, rel, stack, tool, test, build, style, runtime)


def collect_detected_files(root: Path) -> list[str]:
    interesting = (
        'package.json',
        'pnpm-lock.yaml',
        'yarn.lock',
        'package-lock.json',
        'pom.xml',
        'build.gradle',
        'build.gradle.kts',
        'gradlew',
        'pyproject.toml',
        'Makefile',
    )
    found: list[str] = []
    for path in root.rglob('*'):
        if should_ignore(path.relative_to(root)):
            continue
        if path.is_file() and path.name in interesting:
            found.append(path.relative_to(root).as_posix())
    return sorted(found)


def scan_project(target: Path) -> ProjectScan:
    root = target.resolve()
    backend = best_area(root, 'backend', '백엔드', score_backend)
    primary = best_area(root, 'primary_frontend', '주요 프론트엔드', score_frontend)
    secondary = best_area(root, 'secondary_app', '보조 앱', score_secondary)

    notes: list[str] = []
    if backend.path == primary.path and backend.path != 'N/A':
        notes.append('backend and primary frontend resolved to the same path; confirm monorepo layout manually')
    if primary.path == secondary.path and primary.path != 'N/A':
        notes.append('primary frontend and secondary app resolved to the same path; mark unused area as N/A if needed')

    return ProjectScan(
        root=root,
        project_name=root.name,
        backend=backend,
        primary_frontend=primary,
        secondary_app=secondary,
        detected_files=collect_detected_files(root),
        notes=notes,
    )


def area_row(area: Area) -> str:
    return f'| {area.label} | `{area.path}` | `{area.stack}` |'


def command_or_na(value: str) -> str:
    return value if value and value != 'N/A' else 'N/A'


def generate_baseline(scan: ProjectScan) -> str:
    backend = scan.backend
    primary = scan.primary_frontend
    secondary = scan.secondary_app
    return f"""# 프로젝트 기준

이 문서는 하네스를 적용한 프로젝트의 고정 기준을 짧게 유지하는 문서다.
`scripts/apply-harness-to-project.py`가 생성한 초안이며, 코드와 다르면 코드를 확인한 뒤 갱신한다.

## 사용 원칙

- 작업 시작 시 기본 컨텍스트로 읽는다.
- 긴 실행 로그, 파일 전체 목록, 일회성 조사 결과를 넣지 않는다.
- 기술 스택, 레이어 경계, 주요 명령, 운영 규칙처럼 반복적으로 필요한 사실만 남긴다.

## 프로젝트 구조

| 영역 | 경로 | 기준 |
|---|---|---|
{area_row(backend)}
{area_row(primary)}
{area_row(secondary)}

필요 없는 영역은 `N/A`로 표시하고 관련 라우팅 문서에서도 사용하지 않는다.

## 아키텍처 기준

### 백엔드

- `presentation` / `application` / `domain` / `infrastructure` 레이어 경계를 유지한다.
- Presentation layer는 요청/응답 변환과 validation만 담당한다.
- 애플리케이션 서비스는 유스케이스 조율과 트랜잭션 경계를 담당한다.
- 도메인 모델은 불변 조건과 핵심 규칙을 보호한다.
- 영속성 구현은 도메인 규칙을 숨기거나 우회하지 않는다.

### 프론트엔드

- 사용자 표시 문구는 프로젝트의 i18n/content 시스템을 사용한다.
- 기존 컴포넌트, 스타일 토큰, 디자인 톤을 우선한다.
- 인라인 스타일과 임의 외부 UI 라이브러리 도입을 피한다.
- 접근성, 반응형, 로딩/빈 상태/오류 상태를 함께 검토한다.

### 통합

- API/인증/리소스/페이지네이션/목록 로딩 변경은 통합 리뷰를 거친다.
- API 계약 변경 시 소비자와 제공자를 함께 확인한다.
- 보조 앱은 행위자 안전 API 경계와 프로필 정의 런타임 경계를 우선한다.

## 기본 검증 명령

| 영역 | 명령 |
|---|---|
| 백엔드 테스트 | `{command_or_na(backend.test_command)}` |
| 백엔드 빌드 | `{command_or_na(backend.build_command)}` |
| 주요 프론트엔드 테스트 | `{command_or_na(primary.test_command)}` |
| 주요 프론트엔드 빌드 | `{command_or_na(primary.build_command)}` |
| 보조 앱 테스트 | `{command_or_na(secondary.test_command)}` |
| 보조 앱 빌드 | `{command_or_na(secondary.build_command)}` |

## 갱신 기준

아래 중 하나가 바뀌면 이 문서를 갱신한다.

- repo 구조 또는 주요 directory
- 기술 stack 또는 빌드/test 명령
- 백엔드 레이어 경계와 공통 정책
- 프론트 디자인 시스템/접근성/i18n 기준
- API/auth/resource/pagination 경계
- 운영/배포/환경 기준
"""


def generate_project_profile(scan: ProjectScan) -> str:
    backend = scan.backend
    primary = scan.primary_frontend
    secondary = scan.secondary_app
    workspace = scan.root.parent.as_posix()
    return f"""# 프로젝트 프로파일

이 문서는 하네스가 적용되는 프로젝트의 구체 맥락을 둔다.
`scripts/apply-harness-to-project.py`가 생성한 초안이며, 프로젝트 용어와 다르면 수정한다.

## 저장소 자리표시자

| Placeholder | 의미 | 현재 값 |
|---|---|---|
| `workspace-root` | 하나 이상의 Git 저장소를 담는 작업 루트 | `{workspace}` |
| `backend-dir` | 백엔드 Git 저장소 또는 package | `{backend.path}` |
| `primary-frontend-dir` | 주요 운영 프론트엔드 저장소 또는 package | `{primary.path}` |
| `secondary-app-dir` | 보조 앱 저장소 또는 package | `{secondary.path}` |

## 런타임 스택

| 영역 | Stack / Runtime | Package manager / Build tool |
|---|---|---|
| 백엔드 | `{backend.stack}` | `{backend.package_tool}` |
| 주요 프론트엔드 | `{primary.stack}` | `{primary.package_tool}` |
| 보조 앱 | `{secondary.stack}` | `{secondary.package_tool}` |

## 행위자와 리소스 용어

| 범용 용어 | 프로젝트 용어 | 메모 |
|---|---|---|
| 주요 행위자 | `N/A` | 프로젝트 적용 후 실제 운영/관리 행위자로 교체 |
| 보조 행위자 | `N/A` | 보조 앱이 없으면 N/A 유지 |
| 공개 행위자 | `N/A` | 익명/공개 흐름이 있으면 작성 |
| 리소스 범위 | `N/A` | 권한과 필터링 기준이 되는 리소스 |
| 소속 / 소유 관계 | `N/A` | 행위자-리소스 접근 검증 방식 |

## API 경계

- 주요 API prefix: `N/A`
- 보조 앱 API prefix: `N/A`
- 공개 API prefix: `N/A`
- legacy route allow-list 위치: `docs/harness/context/integration/api-matrix.md`
- redaction policy: `민감 필드, 원본 토큰, 비공개 URL, 저장 경로, 구현 전용 ID는 명시 허용되지 않는 한 제외한다.`

## 백엔드 패키지 경계

권장 표현 계층 분리 템플릿:

```text
N/A
```

## 검증 명령

기본 검증 명령은 `docs/harness/context/BASELINE.md`와 일치시킨다.
사용하지 않는 영역은 `N/A`로 둔다.

| 영역 | 명령 |
|---|---|
| 백엔드 전체 테스트 | `{command_or_na(backend.test_command)}` |
| 백엔드 대상 테스트 | `N/A` |
| 백엔드 빌드 | `{command_or_na(backend.build_command)}` |
| 백엔드 런타임/의존성 확인 | `{command_or_na(backend.runtime_command)}` |
| 주요 프론트엔드 테스트 | `{command_or_na(primary.test_command)}` |
| 주요 프론트엔드 대상 테스트 | `N/A` |
| 주요 프론트엔드 빌드 | `{command_or_na(primary.build_command)}` |
| 주요 프론트엔드 스타일/타입 확인 | `{command_or_na(primary.style_command)}` |
| 보조 앱 테스트 | `{command_or_na(secondary.test_command)}` |
| 보조 앱 빌드 | `{command_or_na(secondary.build_command)}` |
| 보조 앱 런타임 확인 | `{command_or_na(secondary.runtime_command)}` |
"""


def generate_design_profile(scan: ProjectScan) -> str:
    return f"""# 디자인 시스템 프로파일

이 문서는 특정 프로젝트의 시각 토큰/테마 이름을 둔다.
`scripts/apply-harness-to-project.py`가 생성한 초안이며, 실제 디자인 시스템 기준으로 보정한다.

## 현재 테마

- 테마 이름: `{scan.project_name}`
- 주요 역할 색상: `N/A`
- 토큰 접두사: `N/A`
- 글꼴 묶음: `N/A`
- 반경 척도: `N/A`
- 간격 척도: `N/A`

## 토큰 이관 규칙

- 새 프로젝트로 이식할 때 토큰 접두사와 호환 별칭을 실제 프로젝트 기준으로 교체한다.
- 기존 코드 호환을 위해 레거시 별칭을 유지할 수 있지만, 새 컴포넌트에서는 의미 역할 토큰을 우선한다.
- 원시 색상 값은 컴포넌트 내부에 직접 넣지 않고 토큰으로 먼저 정의한다.

## 역할 분리

- 주요 CTA 색상과 인라인 링크 색상은 같은 색 계열이어도 역할을 분리한다.
- 의미 상태(success/warning/error)는 브랜드 강조와 분리한다.
- 어두운 화면 영역의 전경 토큰은 별도 어두운 배경용 토큰을 사용한다.
- 포커스 링, 선택 상태, 파괴적 행동, 비활성 상태는 각각 독립 역할을 둔다.
"""


def yaml_area(area: Area, key: str) -> str:
    return f"""  {key}:
    path: {area.path}
    stack: {area.stack}
"""


def replace_yaml_section(text: str, start_key: str, end_key: str, replacement: str) -> str:
    pattern = re.compile(rf'^{re.escape(start_key)}:\n.*?(?=^{re.escape(end_key)}:\n)', re.M | re.S)
    if not pattern.search(text):
        raise ValueError(f'harness.yaml missing section: {start_key}')
    return pattern.sub(replacement.rstrip() + '\n\n', text, count=1)


def generate_harness_yaml(scan: ProjectScan, source: Path) -> str:
    template_path = source / 'docs/harness/harness.yaml'
    if not template_path.exists():
        raise ValueError(f'missing source harness.yaml: {template_path}')
    text = template_path.read_text(encoding='utf-8')
    project_section = f"""project:
  name: {scan.project_name}
  maintainer: N/A
  workspace_root: {scan.root.parent.as_posix()}
  mode: project
"""
    repositories_section = f"""repositories:
{yaml_area(scan.backend, 'backend')}{yaml_area(scan.primary_frontend, 'primary_frontend')}{yaml_area(scan.secondary_app, 'secondary_app')}"""
    text = replace_yaml_section(text, 'project', 'repositories', project_section)
    text = replace_yaml_section(text, 'repositories', 'source_of_truth', repositories_section)
    return text


def generated_profiles(scan: ProjectScan, source: Path) -> dict[Path, str]:
    harness_yaml = generate_harness_yaml(scan, source)
    return {
        Path('MANIFEST.md'): generate_manifest(harness_yaml, source),
        Path('docs/harness/context/BASELINE.md'): generate_baseline(scan),
        Path('docs/harness/profiles/project-profile.md'): generate_project_profile(scan),
        Path('docs/harness/profiles/design-system-profile.md'): generate_design_profile(scan),
        Path('docs/harness/harness.yaml'): harness_yaml,
    }


def generate_manifest(harness_yaml: str, source: Path) -> str:
    source_manifest = source / 'MANIFEST.md'
    if not source_manifest.exists():
        return harness_yaml
    text = source_manifest.read_text(encoding='utf-8')
    marker = '\n## '
    if marker not in text:
        return harness_yaml
    suffix = '## ' + text.split(marker, 1)[1]
    return harness_yaml.rstrip() + '\n\n' + suffix


def iter_source_files(source: Path, rel_dir: str) -> Iterable[Path]:
    start = source / rel_dir
    if not start.exists():
        return []
    return (path for path in start.rglob('*') if path.is_file() and not should_ignore(path.relative_to(source)))


def ensure_parent(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def write_text(path: Path, text: str) -> None:
    ensure_parent(path)
    path.write_text(text, encoding='utf-8')


def copy_file(source_file: Path, target_file: Path) -> None:
    ensure_parent(target_file)
    shutil.copy2(source_file, target_file)


def maybe_copy(result: AdoptionResult, source_file: Path, rel: Path, overwrite: bool = True) -> None:
    target_file = result.target / rel
    label = rel.as_posix()
    if result.apply:
        if target_file.exists() and not overwrite:
            result.conflicts.append(label)
            return
        copy_file(source_file, target_file)
        result.written.append(label)
    else:
        if target_file.exists() and not overwrite:
            result.conflicts.append(label)
        else:
            result.planned.append(label)


def copy_managed(result: AdoptionResult) -> None:
    if result.profile_only:
        result.notes.append('profile-only mode skipped managed file copy')
        return

    for rel_name in ROOT_ALWAYS_COPY:
        source_file = result.source / rel_name
        if source_file.exists():
            maybe_copy(result, source_file, Path(rel_name), overwrite=True)

    for rel_name in ROOT_COPY_IF_MISSING:
        source_file = result.source / rel_name
        if source_file.exists():
            maybe_copy(result, source_file, Path(rel_name), overwrite=False)

    for rel_dir in MANAGED_DIRS:
        for source_file in iter_source_files(result.source, rel_dir):
            rel = source_file.relative_to(result.source)
            target_file = result.target / rel
            if rel in PROFILE_PATHS:
                continue
            if rel == Path('.github/CODEOWNERS') and target_file.exists():
                result.conflicts.append(rel.as_posix())
                continue
            if is_project_owned(rel) and target_file.exists():
                result.skipped.append(f'{rel.as_posix()} (project-owned existing file)')
                continue
            if target_file.exists() and rel.parts[0] in {'.agents', '.codex', '.claude', '.github', 'scripts', 'tests'}:
                maybe_copy(result, source_file, rel, overwrite=True)
            elif target_file.exists() and len(rel.parts) >= 2 and rel.parts[:2] == ('docs', 'harness'):
                maybe_copy(result, source_file, rel, overwrite=True)
            elif target_file.exists():
                result.skipped.append(f'{rel.as_posix()} (existing)')
            else:
                maybe_copy(result, source_file, rel, overwrite=True)


def write_profiles(result: AdoptionResult, overwrite_profiles: bool) -> None:
    for rel, text in generated_profiles(result.scan, result.source).items():
        target_file = result.target / rel
        label = rel.as_posix()
        should_write = overwrite_profiles or has_placeholders(target_file)
        if not should_write:
            result.skipped.append(f'{label} (existing profile without placeholders)')
            continue
        if result.apply:
            write_text(target_file, text)
            result.written.append(label)
        else:
            result.planned.append(label)


def adapt_project_makefile(result: AdoptionResult) -> None:
    makefile = result.target / 'Makefile'
    if not result.apply or result.profile_only or not makefile.exists():
        return
    text = makefile.read_text(encoding='utf-8')
    updated = text.replace('verify: verify-template verify-project', 'verify: verify-project')
    updated = updated.replace('Run template and project harness verification', 'Run project harness verification')
    if updated != text:
        makefile.write_text(updated, encoding='utf-8')
        result.written.append('Makefile (project verify mode)')


def apply_harness(
    source: Path,
    target: Path,
    *,
    apply: bool = False,
    profile_only: bool = False,
    overwrite_profiles: bool = False,
) -> AdoptionResult:
    source = source.resolve()
    target = target.resolve()
    if not source.exists():
        raise ValueError(f'source does not exist: {source}')
    if not target.exists() or not target.is_dir():
        raise ValueError(f'target must be an existing directory: {target}')
    if source == target:
        raise ValueError('source and target must be different directories')

    scan = scan_project(target)
    result = AdoptionResult(source=source, target=target, apply=apply, profile_only=profile_only, scan=scan)
    result.notes.extend(scan.notes)
    copy_managed(result)
    write_profiles(result, overwrite_profiles=overwrite_profiles)
    adapt_project_makefile(result)
    return result


def scan_to_dict(scan: ProjectScan) -> dict[str, object]:
    def area_to_dict(area: Area) -> dict[str, str]:
        return {
            'path': area.path,
            'stack': area.stack,
            'package_tool': area.package_tool,
            'test_command': area.test_command,
            'build_command': area.build_command,
            'style_command': area.style_command,
            'runtime_command': area.runtime_command,
        }

    return {
        'root': str(scan.root),
        'project_name': scan.project_name,
        'backend': area_to_dict(scan.backend),
        'primary_frontend': area_to_dict(scan.primary_frontend),
        'secondary_app': area_to_dict(scan.secondary_app),
        'detected_files': scan.detected_files,
        'notes': scan.notes,
    }


def print_text_report(result: AdoptionResult) -> None:
    mode = 'apply' if result.apply else 'dry-run'
    print(f'[INFO] apply-harness mode: {mode}')
    print(f'[INFO] source: {result.source}')
    print(f'[INFO] target: {result.target}')
    print(f'[INFO] project: {result.scan.project_name}')
    for key, area in (
        ('backend', result.scan.backend),
        ('primary_frontend', result.scan.primary_frontend),
        ('secondary_app', result.scan.secondary_app),
    ):
        print(f'[SCAN] {key}: path={area.path} stack={area.stack} tool={area.package_tool}')
    for item in result.planned:
        print(f'[PLAN] {item}')
    for item in result.written:
        print(f'[WRITE] {item}')
    for item in result.skipped:
        print(f'[SKIP] {item}')
    for item in result.conflicts:
        print(f'[CONFLICT] {item}')
    for item in result.notes:
        print(f'[NOTE] {item}')
    if not result.apply:
        print('[INFO] dry-run only; rerun with --apply to write files')


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description='Apply ninja-harness to another repository and generate project profiles.')
    parser.add_argument('--target', required=True, help='Target project repository root.')
    parser.add_argument('--source', default=str(ROOT), help='Source ninja-harness root. Defaults to this repository.')
    parser.add_argument('--apply', action='store_true', help='Write files. Omit for dry-run report only.')
    parser.add_argument('--profile-only', action='store_true', help='Only generate profile/context files; skip managed file copy.')
    parser.add_argument('--overwrite-profiles', action='store_true', help='Overwrite existing profile/context files even without placeholders.')
    parser.add_argument('--json', action='store_true', help='Print machine-readable JSON report.')
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    try:
        result = apply_harness(
            Path(args.source),
            Path(args.target),
            apply=args.apply,
            profile_only=args.profile_only,
            overwrite_profiles=args.overwrite_profiles,
        )
    except ValueError as exc:
        print(f'[FAIL] {exc}', file=sys.stderr)
        return 1

    if args.json:
        print(json.dumps(result.to_dict(), ensure_ascii=False, indent=2, sort_keys=True))
    else:
        print_text_report(result)
    return 0
