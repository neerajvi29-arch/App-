package com.example.data.util

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
import com.example.MainActivity

class NotificationHelper(private val context: Context) {

  companion object {
    const val CHANNEL_GOAL_REMINDERS = "channel_goal_reminders"
    const val CHANNEL_MILESTONES = "channel_milestones"
    const val NOTIFICATION_ID_GOAL = 1001
    const val NOTIFICATION_ID_MILESTONE = 1002
  }

  init {
    createNotificationChannels()
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      val goalChannel = NotificationChannel(
        CHANNEL_GOAL_REMINDERS,
        "Daily Goal Reminders",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Encouraging reminders to help you hit your daily step goals"
        enableVibration(true)
      }

      val milestoneChannel = NotificationChannel(
        CHANNEL_MILESTONES,
        "Milestones & Achievements",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Celebrations when you unlock badges and streaks"
        enableVibration(true)
      }

      notificationManager.createNotificationChannel(goalChannel)
      notificationManager.createNotificationChannel(milestoneChannel)
    }
  }

  private fun canSendNotifications(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val permission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      )
      if (permission != PackageManager.PERMISSION_GRANTED) {
        return false
      }
    }
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
  }

  fun sendGoalReminder(currentSteps: Long, goal: Long) {
    if (!canSendNotifications()) return

    val remaining = (goal - currentSteps).coerceAtLeast(0)
    val percentage = if (goal > 0) ((currentSteps.toFloat() / goal) * 100).toInt() else 0

    val title = if (remaining == 0L) {
      "🎉 Goal Crushed Today!"
    } else {
      "👟 Keep moving! $percentage% of goal reached"
    }

    val content = if (remaining == 0L) {
      "You've completed $currentSteps steps today! Amazing dedication."
    } else {
      "Only $remaining steps left to hit your daily target of $goal. Let's take a quick walk!"
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_GOAL_REMINDERS)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setContentTitle(title)
      .setContentText(content)
      .setStyle(NotificationCompat.BigTextStyle().bigText(content))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .setProgress(goal.toInt(), currentSteps.toInt(), false)
      .build()

    try {
      NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_GOAL, notification)
    } catch (_: SecurityException) {
      // Handled gracefully when permission is absent
    }
  }

  fun sendMilestoneUnlocked(title: String, description: String) {
    if (!canSendNotifications()) return

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_MILESTONES)
      .setSmallIcon(android.R.drawable.star_on)
      .setContentTitle("🏆 Achievement Unlocked: $title")
      .setContentText(description)
      .setStyle(NotificationCompat.BigTextStyle().bigText("$description\nCheck your achievements trophy case in Step Tracker!"))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .build()

    try {
      NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MILESTONE, notification)
    } catch (_: SecurityException) {
      // Handled gracefully
    }
  }
}
