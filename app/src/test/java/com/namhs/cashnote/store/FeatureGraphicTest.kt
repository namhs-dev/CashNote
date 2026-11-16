package com.namhs.cashnote.store

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import com.namhs.cashnote.R
import com.namhs.cashnote.presentation.home.HomeScreen
import com.namhs.cashnote.presentation.theme.CashNoteTheme
import com.namhs.cashnote.presentation.theme.GreenPrimaryContainerLight
import com.namhs.cashnote.presentation.theme.GreenPrimaryLight
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Play 스토어 그래픽 이미지(1024×500)를 만든다. 실행 방법은 [StoreScreenshotTest]와 같다.
 *
 * w512dp-h250dp-xhdpi: 512×250dp × 2배 = 1024×500px
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "ko-rKR-w512dp-h250dp-xhdpi")
class FeatureGraphicTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun featureGraphic() {
        composeRule.setContent {
            CashNoteTheme(darkTheme = false) { FeatureGraphic() }
        }
        composeRule.onRoot().captureRoboImage("../docs/store/feature-graphic.png")
    }
}

@Composable
private fun FeatureGraphic() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(GreenPrimaryLight, Color(0xFF0E4A3E)))
            )
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppIcon()
            Column {
                Text(
                    text = "CashNote",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "기록은 내 기기에만",
                    color = GreenPrimaryContainerLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "간편한 오프라인 가계부",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        // 오른쪽에 내역 화면 윗부분을 휴대전화처럼 띄운다. 아래는 이미지 밖으로 잘린다
        PhoneFrame(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-28).dp, y = 28.dp)
        )
    }
}

/** 런처 아이콘처럼 배경·전경을 겹치고, 마스크에 잘리는 바깥 여백(108 중 18)을 뺀다 */
@Composable
private fun AppIcon() {
    val size = 64.dp
    Box(
        Modifier
            .size(size)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        val full = size * 108 / 72
        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.requiredSize(full))
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(full))
    }
}

/** 360×640dp 화면을 [SCALE]배로 줄여 둥근 테두리 안에 그린다 */
@Composable
private fun PhoneFrame(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    val bezel = 4.dp
    Box(
        modifier
            // 화면 크기에 양쪽 테두리 두께를 더해야 화면 오른쪽이 잘리지 않는다
            .size(360.dp * SCALE + bezel * 2, 640.dp * SCALE + bezel * 2)
            .shadow(12.dp, shape)
            .border(bezel, Color(0xFF171D1B), shape)
            .padding(bezel)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Box(
            Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .size(360.dp, 640.dp)
                .graphicsLayer {
                    scaleX = SCALE
                    scaleY = SCALE
                    transformOrigin = TransformOrigin(0f, 0f)
                }
        ) {
            HomeScreen(state = StoreSampleData.home, onIntent = {})
        }
    }
}

private const val SCALE = 0.5f
