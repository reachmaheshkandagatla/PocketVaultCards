package com.mahesh.pocketvault

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mahesh.pocketvault.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GroceryReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val folderId = intent.getLongExtra(GroceryReminderScheduler.EXTRA_FOLDER_ID, -1L)
        val folderName = intent.getStringExtra(GroceryReminderScheduler.EXTRA_FOLDER_NAME) ?: "Groceries"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (folderId > 0 && GroceryReminderScheduler.isEnabled(context, folderId)) {
                    val pendingCount = AppDatabase.get(context).groceryItemDao().pendingCountByFolder(folderId)
                    if (pendingCount > 0) {
                        showNotification(context, folderId, folderName, pendingCount)
                    }
                    GroceryReminderScheduler.schedule(context, folderId, folderName)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, folderId: Long, folderName: String, pendingCount: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                GroceryReminderScheduler.CHANNEL_ID,
                "Grocery reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            folderId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val itemLabel = if (pendingCount == 1) "item" else "items"
        val notification = NotificationCompat.Builder(context, GroceryReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_grocery_notification)
            .setContentTitle("Grocery reminder")
            .setContentText("$folderName still has $pendingCount unchecked $itemLabel.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(folderId.toInt(), notification)
    }
}
