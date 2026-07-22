package com.mahesh.pocketvault

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import java.util.Calendar

object GroceryReminderScheduler {
    const val CHANNEL_ID = "purchase_reminders_v2"
    const val EXTRA_FOLDER_ID = "folder_id"
    const val EXTRA_FOLDER_NAME = "folder_name"
    private const val PREFS_NAME = "grocery_reminders"
    private const val DEFAULT_REMINDER_HOUR = 16
    private const val DEFAULT_REMINDER_MINUTE = 0

    data class ReminderTime(val hour: Int, val minute: Int)

    fun isEnabled(context: Context, folderId: Long): Boolean {
        return prefs(context).getBoolean(enabledKey(folderId), false)
    }

    fun setEnabled(context: Context, folderId: Long, folderName: String, enabled: Boolean) {
        prefs(context).edit {
            putBoolean(enabledKey(folderId), enabled)
            putString(nameKey(folderId), folderName)
        }

        if (enabled) {
            schedule(context, folderId, folderName)
        } else {
            cancel(context, folderId)
        }
    }

    fun getReminderTime(context: Context, folderId: Long): ReminderTime {
        val prefs = prefs(context)
        return ReminderTime(
            hour = prefs.getInt(hourKey(folderId), DEFAULT_REMINDER_HOUR),
            minute = prefs.getInt(minuteKey(folderId), DEFAULT_REMINDER_MINUTE)
        )
    }

    fun setReminderTime(context: Context, folderId: Long, folderName: String, hour: Int, minute: Int) {
        prefs(context).edit {
            putInt(hourKey(folderId), hour)
            putInt(minuteKey(folderId), minute)
            putString(nameKey(folderId), folderName)
        }

        if (isEnabled(context, folderId)) {
            schedule(context, folderId, folderName)
        }
    }

    fun formatReminderTime(time: ReminderTime): String {
        val hour = time.hour % 12
        val displayHour = if (hour == 0) 12 else hour
        val displayMinute = time.minute.toString().padStart(2, '0')
        val period = if (time.hour < 12) "AM" else "PM"
        return "$displayHour:$displayMinute $period"
    }

    fun schedule(context: Context, folderId: Long, folderName: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderIntent(context, folderId, folderName)
        val triggerAt = nextReminderTimeMillis(context, folderId)

        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun cancel(context: Context, folderId: Long) {
        val folderName = prefs(context).getString(nameKey(folderId), "Groceries") ?: "Groceries"
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(reminderIntent(context, folderId, folderName))
    }

    private fun reminderIntent(context: Context, folderId: Long, folderName: String): PendingIntent {
        val intent = Intent(context, GroceryReminderReceiver::class.java).apply {
            putExtra(EXTRA_FOLDER_ID, folderId)
            putExtra(EXTRA_FOLDER_NAME, folderName)
        }
        return PendingIntent.getBroadcast(
            context,
            folderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextReminderTimeMillis(context: Context, folderId: Long): Long {
        val now = Calendar.getInstance()
        val reminderTime = getReminderTime(context, folderId)
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminderTime.hour)
            set(Calendar.MINUTE, reminderTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private fun enabledKey(folderId: Long) = "enabled_$folderId"
    private fun nameKey(folderId: Long) = "name_$folderId"
    private fun hourKey(folderId: Long) = "hour_$folderId"
    private fun minuteKey(folderId: Long) = "minute_$folderId"
}
