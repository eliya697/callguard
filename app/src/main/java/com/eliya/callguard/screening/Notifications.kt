package com.eliya.callguard.screening

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.eliya.callguard.R
import com.eliya.callguard.data.PhoneNumbers
import com.eliya.callguard.ui.MainActivity

object Notifications {

    private const val CHANNEL_BLOCKED = "blocked_calls"
    private const val CHANNEL_HANGUP = "scheduled_hangup"
    const val HANGUP_NOTIFICATION_ID = 1001
    private const val ACCENT = 0xFF1E3A5F.toInt()

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_BLOCKED,
                context.getString(R.string.channel_blocked),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.channel_blocked_desc) },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_HANGUP,
                context.getString(R.string.channel_hangup),
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = context.getString(R.string.channel_hangup_desc)
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    @SuppressLint("MissingPermission") // Guarded by areNotificationsEnabled().
    fun showBlocked(context: Context, name: String, number: String) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_BLOCKED)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setColor(ACCENT)
            .setContentTitle("שיחה נחסמה · $name")
            .setContentText(PhoneNumbers.pretty(number))
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setContentIntent(openHistory(context))
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .build()
        nm.notify(number.hashCode(), notification)
    }

    fun hangUpPending(context: Context, name: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_HANGUP)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setColor(ACCENT)
            .setContentTitle("ניתוק מתוזמן")
            .setContentText(if (name.isBlank()) "השיחה תנותק בקרוב" else "השיחה מ$name תנותק בקרוב")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .build()

    private fun openHistory(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_HISTORY, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
