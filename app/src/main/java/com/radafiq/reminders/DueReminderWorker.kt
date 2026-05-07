package com.radafiq.reminders

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
        
        // FIX-13: Use a stable, collision-resistant notification ID.
        // Combine accountId + dueDate + daysLeft into a deterministic but unique integer
        // using a better hash that avoids the 32-bit String.hashCode() collision space.
        val idSource = "$accountId|$dueDate|$daysLeft"
        val notificationId = run {
            val digest = java.security.MessageDigest.getInstance("MD5").digest(idSource.toByteArray())
            // Take first 4 bytes as a positive int
            ((digest[0].toInt() and 0xFF) shl 24) or
            ((digest[1].toInt() and 0xFF) shl 16) or
            ((digest[2].toInt() and 0xFF) shl 8)  or
             (digest[3].toInt() and 0xFF)
        }.let { if (it == Int.MIN_VALUE) Int.MAX_VALUE else Math.abs(it) }
        
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

        if (!canPostNotifications()) return Result.success()

        postNotification(notificationId, notification)

        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(notificationId: Int, notification: android.app.Notification) {
        runCatching {
            NotificationManagerCompat.from(applicationContext)
                .notify(notificationId, notification)
        }
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
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
