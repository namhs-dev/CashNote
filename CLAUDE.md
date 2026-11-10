# CashNote

오프라인 우선 가계부 Android 앱. 포트폴리오용 1인 프로젝트.

## 기술 스택
- Kotlin, Jetpack Compose (Material3)
- 아키텍처: MVI + Clean Architecture (presentation / domain / data)
- DI: Hilt
- 로컬 DB: Room (Single Source of Truth)
- 네트워크: Retrofit2 + OkHttp + kotlinx.serialization (한국수출입은행 환율 API)
- 비동기: Coroutines + Flow
- 패키지: `com.namhs.cashnote`, 단일 `:app` 모듈, minSdk 26

## 빌드 / 테스트
- 빌드: `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`)
- 단위 테스트: `./gradlew testDebugUnitTest`
- 계측 테스트: `./gradlew connectedDebugAndroidTest`
- 코드 수정 후 커밋 제안 전에 빌드가 통과하는지 확인한다.

## 패키지 구조
```
com.namhs.cashnote
├─ core/            # 공통 유틸, 확장 함수, Result 타입
├─ data/
│  ├─ local/        # Room: entity, dao, database, converter
│  ├─ remote/       # Retrofit: api, dto
│  └─ repository/   # Repository 구현체, mapper
├─ domain/
│  ├─ model/        # 순수 Kotlin 도메인 모델
│  ├─ repository/   # Repository 인터페이스
│  └─ usecase/      # UseCase (기능 1개 = 클래스 1개)
├─ presentation/
│  ├─ <feature>/    # Screen, ViewModel, Contract(State/Intent/Effect)
│  └─ theme/
└─ di/              # Hilt 모듈
```

## 아키텍처 규칙
- domain은 Android/Room/Retrofit 의존성을 갖지 않는다.
- UI 이벤트 처리 흐름: Screen → Intent → ViewModel → UseCase → Repository.
- ViewModel은 Repository를 직접 호출하지 않고 UseCase만 호출한다.
- UseCase는 `operator fun invoke()` 하나만 노출한다.
- MVI Contract: 화면마다 `UiState`(data class, 불변), `Intent`(sealed interface), `SideEffect`(sealed interface, 일회성 이벤트).
- State는 `StateFlow`, SideEffect는 `Channel` → `Flow`로 노출한다.
- 화면 ViewModel은 `presentation/base/MviViewModel`을 상속하고, Intent는 `onIntent()` 하나로 받는다.
- Contract는 `presentation/<feature>/<Feature>Contract.kt` 한 파일에 `<Feature>UiState` / `<Feature>Intent` / `<Feature>SideEffect`로 둔다.
- State는 `setState { copy(...) }`로만 바꾼다. 내비게이션·스낵바 같은 일회성 이벤트는 State에 넣지 않고 `postSideEffect()`로 보낸다.
- Route는 State를 `collectAsStateWithLifecycle()`로 구독하고, SideEffect는 `LaunchedEffect`에서 수집한다. Screen에는 State와 `onIntent` 람다만 넘긴다.
- Room이 유일한 데이터 소스다. 네트워크 응답은 Room에 저장하고, UI는 Room Flow만 구독한다.
- Entity ↔ Domain, DTO ↔ Entity 변환은 data 계층의 mapper에서만 한다.
- API 키는 `local.properties` → `BuildConfig`로 주입한다. 키를 코드나 커밋에 넣지 않는다.

## 코드 스타일
- Kotlin 공식 코드 스타일을 따른다.
- Composable은 상태를 받는 Stateless Screen과 ViewModel을 연결하는 Route로 나눈다.
- 문자열은 `strings.xml`에 둔다.
- 주석과 커밋 메시지는 한국어로 작성한다.

## Git 규칙
- 브랜치: `feature/<이슈번호>-<설명>`, `fix/<이슈번호>-<설명>`. 항상 `dev`에서 분기한다.
- `main`, `dev`에 직접 커밋하거나 push하지 않는다.
- 커밋 메시지: `<type>: <설명> (#이슈번호)`, type은 feat / fix / refactor / style / chore / docs / test.
- PR 대상은 `dev`, 본문 첫 줄에 `Closes #<이슈번호>`.
- PR 제목: `<type>: <설명>` 형식으로 쓰고, 이슈 번호를 넣지 않는다. squash 머지 시 PR 제목이 dev 커밋 메시지가 되고 PR 번호는 GitHub이 붙인다. CI(PR Title)가 검사한다.
- 이슈 제목: type 접두어 없이 할 일만 쓴다. 종류는 라벨로 구분한다.

## 작업 방식 (중요)
- 작업은 아주 작은 단위로 나눠서 진행한다. 한 번에 한 단계만 한다.
- 코드를 수정하기 전에 무엇을 바꿀지 먼저 설명하고 확인을 받는다.
- 커밋은 직접 하지 않고, 변경 사항을 요약하고 커밋 메시지를 제안한 뒤 멈춘다.
- 이슈 범위를 벗어나는 수정은 하지 않는다. 필요하면 별도 이슈로 제안한다.
- 새 라이브러리는 `gradle/libs.versions.toml`에 추가하고, 추가 이유를 설명한다.
