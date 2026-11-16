package com.namhs.cashnote.presentation.settings

import androidx.annotation.StringRes
import com.namhs.cashnote.domain.model.ExchangeRate
import com.namhs.cashnote.domain.model.NotificationSettings
import java.time.LocalTime

/** 설정 화면 상태 */
data class SettingsUiState(
    /** 새 거래 입력 화면이 처음 고를 통화 코드. null이면 원화 */
    val defaultCurrency: String? = null,
    /** 저장된 환율 목록. 기본 통화 선택지와 통화 이름 표시에 쓴다 */
    val exchangeRates: List<ExchangeRate> = emptyList(),
    /** 기록 리마인더·예산 알림 설정 */
    val notification: NotificationSettings = NotificationSettings(),
    /**
     * 이 앱의 알림이 실제로 표시될 수 있다(Android 13+ 권한 허용, 앱 알림 켜짐).
     * 화면이 보일 때마다 Route가 확인해 알려 준다
     */
    val canPostNotifications: Boolean = true,
    /** 시스템 권한 창을 더 띄울 수 없어 설정 앱으로 안내하는 다이얼로그 */
    val isPermissionSettingsDialogVisible: Boolean = false,
    /** 앱 버전 이름(BuildConfig.VERSION_NAME). 앱 정보 묶음에 보여 준다 */
    val appVersion: String = ""
) {
    /** 기본 통화로 고를 수 있는 외화. 저장된 환율 통화에, 목록에 없는 현재 기본 통화를 더한다 */
    val foreignCurrencyOptions: List<String>
        get() = (exchangeRates.map { it.currencyCode } + listOfNotNull(defaultCurrency)).distinct()

    /** 알림을 켜 두었는데 시스템에서 막혀 받을 수 없다 */
    val isNotificationBlocked: Boolean
        get() = !canPostNotifications &&
            (notification.reminderEnabled || notification.budgetAlertEnabled)
}

/** 켜려고 한 알림 종류. 권한을 얻은 뒤 무엇을 켤지 기억하는 데 쓴다 */
enum class NotificationTarget { REMINDER, BUDGET_ALERT }

/** 설정 화면에서 발생하는 사용자 의도 */
sealed interface SettingsIntent {
    /** 기본 통화 선택. null이면 원화 */
    data class DefaultCurrencySelected(val currencyCode: String?) : SettingsIntent

    data object CategoryManageClicked : SettingsIntent

    data object OssLicensesClicked : SettingsIntent

    /** 앱 정보의 "문의하기" */
    data object ContactClicked : SettingsIntent

    /** 메일 앱이 없어 문의 메일을 열지 못했다고 Route가 알려 준다 */
    data object ContactEmailUnavailable : SettingsIntent

    /** 기록 리마인더 켜기/끄기 */
    data class ReminderToggled(val enabled: Boolean) : SettingsIntent

    /** 리마인더 시각 선택. 꺼져 있어도 저장해 두고 켤 때 쓴다 */
    data class ReminderTimeSelected(val time: LocalTime) : SettingsIntent

    /** 예산 알림 켜기/끄기 */
    data class BudgetAlertToggled(val enabled: Boolean) : SettingsIntent

    /** 화면이 다시 보일 때 Route가 확인한 알림 표시 가능 여부 */
    data class NotificationPermissionChecked(val canPost: Boolean) : SettingsIntent

    /**
     * 권한 요청 결과.
     *
     * @param canAskAgain 거부했지만 시스템 권한 창을 다시 띄울 수 있다. false면 "다시 묻지 않기" 상태라
     * 설정 앱에서만 허용할 수 있다
     */
    data class NotificationPermissionResult(
        val target: NotificationTarget,
        val granted: Boolean,
        val canAskAgain: Boolean
    ) : SettingsIntent

    /** 설정 안내 다이얼로그의 "설정 열기" 또는 차단 안내 문구 */
    data object OpenNotificationSettingsClicked : SettingsIntent

    data object PermissionSettingsDialogDismissed : SettingsIntent
}

/** 설정 화면의 일회성 이벤트 */
sealed interface SettingsSideEffect {
    data object NavigateToCategoryManage : SettingsSideEffect

    data object NavigateToOssLicenses : SettingsSideEffect

    /** 받는 주소·제목·앱과 기기 정보를 채운 문의 메일 작성 화면을 연다 */
    data object OpenContactEmail : SettingsSideEffect

    /** 메일 앱이 없다. 문의 주소를 알려 주고 복사할 수 있게 한다 */
    data object ShowContactEmailUnavailable : SettingsSideEffect

    /** 알림을 켜려면 권한이 필요하다. Route가 권한 창을 띄우고 결과를 돌려준다 */
    data class RequestNotificationPermission(val target: NotificationTarget) :
        SettingsSideEffect

    /** 이 앱의 시스템 알림 설정 화면을 연다 */
    data object OpenNotificationSettings : SettingsSideEffect

    data class ShowMessage(@StringRes val messageRes: Int) : SettingsSideEffect
}
