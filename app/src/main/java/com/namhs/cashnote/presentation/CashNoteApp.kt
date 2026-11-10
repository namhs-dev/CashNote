package com.namhs.cashnote.presentation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.namhs.cashnote.presentation.home.HomeScreen
import com.namhs.cashnote.presentation.navigation.CashNoteDestination
import com.namhs.cashnote.presentation.navigation.TopLevelDestination
import com.namhs.cashnote.presentation.statistics.StatisticsScreen
import com.namhs.cashnote.presentation.transaction.TransactionEditRoute

@Composable
fun CashNoteApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    // 현재 화면이 탭이 아니면(입력 화면 등) null → 하단 바를 숨긴다
    val currentTab = TopLevelDestination.entries.firstOrNull { tab ->
        currentDestination?.hierarchy?.any { it.hasRoute(tab.destination::class) } == true
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (currentTab != null) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = { navController.navigateToTab(tab) },
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
        },
        // 시스템 바 여백은 각 화면이 직접 처리한다
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CashNoteDestination.Home,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<CashNoteDestination.Home> {
                HomeScreen(
                    onAddTransactionClick = {
                        navController.navigate(CashNoteDestination.TransactionEdit())
                    }
                )
            }
            composable<CashNoteDestination.Statistics> { StatisticsScreen() }
            composable<CashNoteDestination.TransactionEdit> {
                TransactionEditRoute(onBack = { navController.navigateUp() })
            }
        }
    }
}

// 탭 전환 시 백스택이 쌓이지 않게 하고, 탭별 상태(스크롤 등)는 저장·복원한다
private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.destination) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
