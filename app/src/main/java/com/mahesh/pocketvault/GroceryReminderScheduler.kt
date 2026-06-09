package com.mahesh.pocketvault

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object GroceryReminderScheduler {
    const val CHANNEL_ID = "grocery_reminders"
    const val EXTRA_FOLDER_ID = "folder_id"
    const val EXTRA_FOLDER_NAME = "folder_name"
    private const val PREFS_NAME = "grocery_reminders"
    private const val REMINDER_HOUR = 20
    private const val REMINDER_MINUTE = 0

    fun isEnabled(context: Context, folderId: Long): Boolean {
        return prefs(context).getBoolean(enabledKey(folderId), false)
    }

    fun setEnabled(context: Context, folderId: Long, folderName: String, enabled: Boolean) {
        prefs(context).edit()
            .putBoolean(enabledKey(folderId), enabled)
            .putString(nameKey(folderId), folderName)
            .apply()

        if (enabled) {
            schedule(context, folderId, folderName)
        } else {
            cancel(context, folderId)
        }
    }

    fun schedule(context: Context, folderId: Long, folderName: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderIntent(context, folderId, folderName)
        val triggerAt = nextReminderTimeMillis()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
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

    private fun nextReminderTimeMillis(): Long {
        val now = Calendar.getInstance()
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(Calendar.MINUTE, REMINDER_MINUTE)
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
}
