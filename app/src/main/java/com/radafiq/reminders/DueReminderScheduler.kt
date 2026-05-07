package com.radafiq.reminders

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

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
        val today = now.toLocalDate()

        // FIX-12: Cap reminder window to 30 days from today to prevent scheduling
        // hundreds of WorkManager jobs when a due date is set far in the future.
        val effectiveStartDate = parsedDueDate.minusDays(5).let { start ->
            if (start.isBefore(today)) today else start
        }
        val effectiveEndDate = parsedDueDate.let { end ->
            val maxEnd = today.plusDays(30)
            if (end.isAfter(maxEnd)) maxEnd else end
        }

        if (effectiveStartDate.isAfter(effectiveEndDate)) return

        generateSequence(effectiveStartDate) { current ->
            if (!current.isAfter(effectiveEndDate)) current.plusDays(1) else null
        }.forEach { reminderDate ->
            val reminderTime = reminderDate.atTime(9, 0)
            if (reminderTime.isBefore(now)) return@forEach

            val workName = workName(accountId, reminderDate)
            val inputData = Data.Builder()
                .putString(DueReminderWorker.KEY_ACCOUNT_ID, accountId)
                .putString(DueReminderWorker.KEY_ACCOUNT_NAME, accountName)
                .putString(DueReminderWorker.KEY_DUE_DATE, parsedDueDate.toString())
                .putInt(
                    DueReminderWorker.KEY_DAYS_LEFT,
                    ChronoUnit.DAYS.between(reminderDate, parsedDueDate).toInt()
                )
                .build()

            val request = OneTimeWorkRequestBuilder<DueReminderWorker>()
                .setInputData(inputData)
                .setInitialDelay(
                    ChronoUnit.MILLIS.between(now, reminderTime).coerceAtLeast(0L),
                    TimeUnit.MILLISECONDS
                )
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
