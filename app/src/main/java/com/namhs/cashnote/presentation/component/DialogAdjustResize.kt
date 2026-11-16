package com.namhs.cashnote.presentation.component

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * 이 다이얼로그 창이 키보드가 올라오면 키보드 위 영역으로 줄어들게 한다.
 *
 * Compose Dialog는 액티비티와 다른 창이라 액티비티의 adjustResize를 따르지 않고, 기본으로는 포커스한 칸이 보일 만큼만
 * 창을 밀어 올린다. 그러면 본문이 긴 다이얼로그(큰 글꼴 포함)는 아래 버튼이 키보드에 가려진다.
 * 창을 줄이고 본문을 스크롤되게 두면 AlertDialog가 본문만 줄이고 버튼은 키보드 위에 남긴다.
 *
 * 다이얼로그 내용(Composable) 안에서 호출한다.
 */
@Composable
fun DialogAdjustResize() {
    // 다이얼로그 내용의 LocalView 부모가 그 다이얼로그 창을 가진 DialogLayout이다
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    DisposableEffect(window) {
        // edge-to-edge가 아닌 다이얼로그 창(decorFitsSystemWindows 기본값)에서는 이 플래그가 그대로 동작한다
        @Suppress("DEPRECATION")
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        onDispose {}
    }
}
