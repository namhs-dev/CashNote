package com.namhs.cashnote.data.notification

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.namhs.cashnote.MainActivity
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.BudgetLevel
import com.namhs.cashnote.domain.repository.Notifier
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

/** 시스템 알림으로 표시한다. 누르면 앱을 연다 */
class AndroidNotifier @Inject constructor(@ApplicationContext private val context: Context) :
    Notifier {
    private val manager = NotificationManagerCompat.from(context)

    override fun showReminder() {
        val notification = NotificationCompat.Builder(context, NotificationChannels.REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_reminder_title))
            .setContentText(context.getString(R.string.notification_reminder_text))
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()
        notifyIfAllowed(REMINDER_ID, notification)
    }

    override fun showBudgetAlert(level: BudgetLevel, budget: Long, spent: Long) {
        val titleRes = when (level) {
            BudgetLevel.NORMAL -> return
            BudgetLevel.WARNING -> R.string.notification_budget_warning_title
            BudgetLevel.EXCEEDED -> R.string.notification_budget_exceeded_title
        }
        val format = NumberFormat.getNumberInstance(Locale.KOREA)
        val text = context.getString(
            R.string.notification_budget_text,
            format.format(spent),
            format.format(budget)
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.BUDGET)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(titleRes))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()
        // 같은 id라 80% 알림이 남아 있으면 100% 알림이 그 자리를 바꾼다
        notifyIfAllowed(BUDGET_ID, notification)
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /** Android 13+에서 권한이 없거나, 사용자가 앱 알림을 꺼 두었으면 보내지 않는다 */
    private fun notifyIfAllowed(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!manager.areNotificationsEnabled()) return
        manager.notify(id, notification)
    }

    private companion object {
        const val REMINDER_ID = 1
        const val BUDGET_ID = 2
    }
}
