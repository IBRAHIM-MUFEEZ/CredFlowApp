package com.radafiq.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.radafiq.MainActivity
import com.radafiq.R

class DueReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        createChannel()

        val accountId = inputData.getString(KEY_ACCOUNT_ID).orEmpty()
        val accountName = inputData.getString(KEY_ACCOUNT_NAME).orEmpty()
        val dueDate = inputData.getString(KEY_DUE_DATE).orEmpty()
        val daysLeft = inputData.getInt(KEY_DAYS_LEFT, 0)

        val title = if (daysLeft == 0) {
            "$accountName due today"
        } else {
            "$accountName due in $daysLeft day${if (daysLeft == 1) "" else "s"}"
        }

        val message = "Due date $dueDate. Open Radafiq to review pending amount and follow up."

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        // Use accountId (stable UUID) instead of accountName to avoid collisions
        val notificationId = (accountId + dueDate + daysLeft).hashCode().let {
            if (it == Int.MIN_VALUE) Int.MAX_VALUE else Math.abs(it)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(notificationId, notification)

        return Result.success()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Due reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily due reminders from 5 days before the due date"
            }

            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "Radafiq_due_reminders"
        const val KEY_ACCOUNT_ID = "account_id"
        const val KEY_ACCOUNT_NAME = "account_name"
        const val KEY_DUE_DATE = "due_date"
        const val KEY_DAYS_LEFT = "days_left"
    }
}
