package com.namhs.cashnote.data.notification

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.namhs.cashnote.R

/**
 * 앱의 알림 채널. 사용자는 시스템 설정에서 채널별로 알림을 끄거나 소리를 바꿀 수 있다.
 *
 * 채널 id는 한 번 만들면 바꾸지 않는다. 바꾸면 사용자가 정한 채널 설정이 사라진다.
 */
object NotificationChannels {
    const val REMINDER = "reminder"
    const val BUDGET = "budget"

    /** 앱 시작 시 호출한다. 이미 있으면 이름·설명만 갱신된다 */
    fun create(context: Context) {
        val reminder = NotificationChannelCompat.Builder(
            REMINDER,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName(context.getString(R.string.notification_channel_reminder))
            .setDescription(context.getString(R.string.notification_channel_reminder_description))
            .build()
        // 예산 초과는 바로 알아야 하므로 헤드업으로 띄운다
        val budget = NotificationChannelCompat.Builder(
            BUDGET,
            NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName(context.getString(R.string.notification_channel_budget))
            .setDescription(context.getString(R.string.notification_channel_budget_description))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannelsCompat(
            listOf(reminder, budget)
        )
    }
}
