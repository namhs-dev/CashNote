# Play 스토어 등록 자료

## 휴대전화 스크린샷

| 파일 | 화면 |
|---|---|
| `screenshots/01-home.png` | 내역: 월 합계·예산, 외화 거래 |
| `screenshots/02-foreign-edit.png` | 입력: 외화(USD)를 원화로 환산 |
| `screenshots/03-statistics.png` | 통계: 카테고리별 지출 도넛 차트 |
| `screenshots/04-category-manage.png` | 카테고리 관리 |
| `screenshots/05-home-dark.png` | 내역 다크 모드 |

모두 1080×1920(9:16), 알파 채널 없는 24비트 PNG로 Play 규격에 맞는다.

### 다시 만들기

기기 없이 JVM에서 Robolectric + Roborazzi로 그린다. 앱 화면 코드가 바뀌면 다시 만든다.

```
gradlew.bat recordRoborazziDebug
```

- 테스트: `app/src/test/java/com/namhs/cashnote/store/StoreScreenshotTest.kt`
- 예시 데이터: 같은 폴더의 `StoreSampleData.kt` (2026년 11월 17일 기준, 기본 카테고리 이름·색)
- 일반 단위 테스트(`testDebugUnitTest`)와 CI에서는 `store` 패키지를 제외한다(`app/build.gradle.kts`). Roborazzi 작업을 실행할 때만 돈다.
- Robolectric은 API 36으로 고정한다. API 37에서는 Espresso가 없어진 `InputManager.getInstance()`를 불러 실패한다.
- 상태 표시줄(시계·배터리)은 그리지 않는다.
