package com.namhs.cashnote.presentation.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

/**
 * 하단 탭 바. 앱([com.namhs.cashnote.presentation.CashNoteApp])과 스토어 스크린샷이 같이 쓴다.
 *
 * @param currentTab 선택 표시할 탭
 */
@Composable
fun CashNoteNavigationBar(
    currentTab: TopLevelDestination,
    onTabClick: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        TopLevelDestination.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == currentTab,
                onClick = { onTabClick(tab) },
                icon = {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(tab.labelRes)) }
            )
        }
    }
}
