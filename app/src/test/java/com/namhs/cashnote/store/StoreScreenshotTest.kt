package com.namhs.cashnote.store

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.namhs.cashnote.presentation.category.CategoryManageScreen
import com.namhs.cashnote.presentation.home.HomeScreen
import com.namhs.cashnote.presentation.navigation.CashNoteNavigationBar
import com.namhs.cashnote.presentation.navigation.TopLevelDestination
import com.namhs.cashnote.presentation.statistics.StatisticsScreen
import com.namhs.cashnote.presentation.theme.CashNoteTheme
import com.namhs.cashnote.presentation.transaction.TransactionEditScreen
import java.util.TimeZone
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Play 스토어 휴대전화 스크린샷(1080×1920, 9:16)을 만든다.
 *
 * 일반 단위 테스트에서는 제외되고 `gradlew recordRoborazziDebug`로만 실행된다(app/build.gradle.kts).
 * 결과는 docs/store/screenshots에 저장된다. 다시 만드는 방법은 docs/store/README.md 참고.
 *
 * - sdk 36: API 37에서는 Espresso가 없어진 InputManager.getInstance()를 불러 실패한다
 * - ko-rKR: 날짜의 요일을 한국어로 표시한다
 * - w360dp-h640dp-xxhdpi: 360×640dp × 3배 = 1080×1920px
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "ko-rKR-w360dp-h640dp-xxhdpi")
class StoreScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    // 환율 갱신 시각을 어느 PC에서 만들어도 한국 시각으로 표시한다
    @Before
    fun useKoreanTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    }

    @Test
    fun home() = capture("01-home") {
        TabFrame(TopLevelDestination.HOME) {
            HomeScreen(state = StoreSampleData.home, onIntent = {})
        }
    }

    @Test
    fun foreignEdit() = capture("02-foreign-edit") {
        TransactionEditScreen(state = StoreSampleData.foreignEdit, onIntent = {})
    }

    @Test
    fun statistics() = capture("03-statistics") {
        TabFrame(TopLevelDestination.STATISTICS) {
            StatisticsScreen(state = StoreSampleData.statistics, onIntent = {})
        }
    }

    @Test
    fun categoryManage() = capture("04-category-manage") {
        CategoryManageScreen(state = StoreSampleData.categoryManage, onIntent = {})
    }

    @Test
    fun homeDark() = capture("05-home-dark", darkTheme = true) {
        TabFrame(TopLevelDestination.HOME) {
            HomeScreen(state = StoreSampleData.home, onIntent = {})
        }
    }

    private fun capture(name: String, darkTheme: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent {
            CashNoteTheme(darkTheme = darkTheme, content = content)
        }
        // 테스트는 app 모듈 폴더에서 실행된다
        composeRule.onRoot().captureRoboImage("../docs/store/screenshots/$name.png")
    }
}

/** 앱의 CashNoteApp처럼 탭 화면 아래에 하단 바를 붙인다 */
@Composable
private fun TabFrame(tab: TopLevelDestination, content: @Composable () -> Unit) {
    Scaffold(
        bottomBar = { CashNoteNavigationBar(currentTab = tab, onTabClick = {}) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) { content() }
    }
}
