# 프로젝트 프로파일

이 문서는 하네스가 적용되는 프로젝트의 구체 맥락을 둔다.
`scripts/apply-harness-to-project.py`가 생성한 초안이며, 프로젝트 용어와 다르면 수정한다.

## 저장소 자리표시자

| Placeholder | 의미 | 현재 값 |
|---|---|---|
| `workspace-root` | 하나 이상의 Git 저장소를 담는 작업 루트 | `/Users/hyunsoojo/IdeaProjects` |
| `backend-dir` | 백엔드 Git 저장소 또는 package | `.` |
| `primary-frontend-dir` | 주요 운영 프론트엔드 저장소 또는 package | `N/A` |
| `secondary-app-dir` | 보조 앱 저장소 또는 package | `N/A` |

## 런타임 스택

| 영역 | Stack / Runtime | Package manager / Build tool |
|---|---|---|
| 백엔드 | `JVM` | `gradle` |
| 주요 프론트엔드 | `N/A` | `N/A` |
| 보조 앱 | `N/A` | `N/A` |

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
| 백엔드 전체 테스트 | `./gradlew test` |
| 백엔드 대상 테스트 | `N/A` |
| 백엔드 빌드 | `./gradlew build` |
| 백엔드 런타임/의존성 확인 | `N/A` |
| 주요 프론트엔드 테스트 | `N/A` |
| 주요 프론트엔드 대상 테스트 | `N/A` |
| 주요 프론트엔드 빌드 | `N/A` |
| 주요 프론트엔드 스타일/타입 확인 | `N/A` |
| 보조 앱 테스트 | `N/A` |
| 보조 앱 빌드 | `N/A` |
| 보조 앱 런타임 확인 | `N/A` |
