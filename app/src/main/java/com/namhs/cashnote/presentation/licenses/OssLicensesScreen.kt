package com.namhs.cashnote.presentation.licenses

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.namhs.cashnote.R
import com.namhs.cashnote.presentation.theme.CashNoteTheme

/**
 * 앱에 들어간 오픈소스 라이브러리와 라이선스 목록.
 *
 * 목록은 빌드할 때 AboutLibraries 플러그인이 res/raw/aboutlibraries.json으로 만든다.
 * 보관할 상태가 없어 ViewModel 없이 Route에서 바로 읽는다.
 */
@Composable
fun OssLicensesRoute(onBack: () -> Unit) {
    // 파일을 백그라운드에서 읽는 동안은 null이다
    val libraries by produceLibraries(R.raw.aboutlibraries)

    OssLicensesScreen(libraries = libraries, onBackClick = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OssLicensesScreen(libraries: Libs?, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.oss_licenses_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_navigate_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // 항목을 누르면 라이선스 전문 다이얼로그가 뜬다
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        )
    }
}

@Preview(name = "오픈소스 라이선스", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun OssLicensesScreenPreview() {
    CashNoteTheme {
        OssLicensesScreen(libraries = null, onBackClick = {})
    }
}
