package com.credflow.reminders

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class DueReminderScheduler(
    private val context: Context
) {
    fun syncDueReminders(
        accountId: String,
        accountName: String,
        dueDate: String,
        enabled: Boolean
    ) {
        cancelDueReminders(accountId)

        if (!enabled) return

        val parsedDueDate = runCatching { LocalDate.parse(dueDate) }.getOrNull() ?: return
        val now = LocalDateTime.now()
        val startDate = parsedDueDate.minusDays(5)

        generateSequence(startDate) { current ->
            if (current.isBefore(parsedDueDate)) current.plusDays(1) else null
        }.forEach { reminderDate ->
            val reminderTime = reminderDate.atTime(9, 0)
            if (reminderTime.isBefore(now)) return@forEach

            val workName = workName(accountId, reminderDate)
            val inputData = Data.Builder()
                .putString(DueReminderWorker.KEY_ACCOUNT_NAME, accountName)
                .putString(DueReminderWorker.KEY_DUE_DATE, parsedDueDate.toString())
                .putInt(
                    DueReminderWorker.KEY_DAYS_LEFT,
                    ChronoUnit.DAYS.between(reminderDate, parsedDueDate).toInt()
                )
                .build()

            val request = OneTimeWorkRequestBuilder<DueReminderWorker>()
                .setInputData(inputData)
                .setInitialDelay(Duration.between(now, reminderTime))
                .addTag(reminderTag(accountId))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    fun cancelDueReminders(accountId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(reminderTag(accountId))
    }

    private fun workName(accountId: String, reminderDate: LocalDate): String {
        return "${reminderTag(accountId)}_${reminderDate}"
    }

    private fun reminderTag(accountId: String): String {
        return "due_reminder_$accountId"
    }
}
