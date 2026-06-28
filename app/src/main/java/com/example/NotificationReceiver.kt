package com.example

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.MIGRATION_5_6
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule alarm on boot
            scheduleDailyNotification(context)
            return
        }

        // Build the message off the main thread (it reads the database), then notify.
        val pending = goAsync()
        Thread {
            try {
                val (title, body) = buildSmartMessage(context)
                showNotification(context, title, body)
            } catch (e: Exception) {
                showNotification(context, "LevelUp ⚡", "Time to check in! Log your habits, sleep and spending 📊")
            } finally {
                pending.finish()
            }
        }.start()
    }

    /** Reads today's progress and crafts a personal, context-aware message. */
    private fun buildSmartMessage(context: Context): Pair<String, String> {
        val name = context.getSharedPreferences("levelup_prefs", Context.MODE_PRIVATE)
            .getString("user_name", "") ?: ""
        val who = if (name.isNotBlank()) name.trim() else "there"
        return try {
            val db = Room.databaseBuilder(context, AppDatabase::class.java, "levelup_db")
                .addMigrations(MIGRATION_5_6).fallbackToDestructiveMigration().build()
            val dao = db.dashboardDao()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val habits = runBlocking { dao.getAllHabits().first() }
            val sleep = runBlocking { dao.getAllSleepLogs().first() }
            db.close()

            val total = habits.size
            val done = habits.count { it.isCompleted }
            val sleptToday = sleep.any { it.dateString == today }

            val tasks = mutableListOf<String>()
            if (total > 0 && done < total) tasks.add("${total - done} habit${if (total - done > 1) "s" else ""} to finish")
            if (!sleptToday) tasks.add("log last night's sleep")

            val body = if (tasks.isEmpty()) {
                "You're on track today, $who! Keep the momentum going 🔥"
            } else {
                "Hey $who — you've still got ${tasks.joinToString(" and ")}. Tap to level up 💪"
            }
            "LevelUp ⚡" to body
        } catch (e: Exception) {
            "LevelUp ⚡" to "Time to check in, $who! Log your habits, sleep and spending 📊"
        }
    }

    private fun showNotification(context: Context, title: String, body: String) {
        val channelId = "daily_tracker_reminder"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Logging Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to log your daily tasks, habits, and sleep activity."
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open MainActivity when notification is clicked
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    companion object {
        fun scheduleDailyNotification(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            // Set up calendar for 11:00 AM
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 11)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                // If it's already past 11:00 AM today, schedule for tomorrow
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // Using inexact repeating to be battery friendly and highly reliable
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }
}
