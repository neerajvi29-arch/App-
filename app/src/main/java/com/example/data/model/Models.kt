package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String? = null,
  val dailyGoal: Long = 10000L,
  val weeklyGoal: Long = 70000L,
  val totalSteps: Long = 0L,
  val currentStreak: Long = 0L,
  val bestStreak: Long = 0L,
  val connectedWearable: String = "Pixel Watch 3",
  @ServerTimestamp val lastSyncTimestamp: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailyLog(
  @DocumentId val id: String = "",
  val date: String = "",
  val userId: String = "",
  val steps: Long = 0L,
  val goal: Long = 10000L,
  val calories: Long = 0L,
  val distanceMeters: Long = 0L,
  val activeMinutes: Long = 0L,
  val source: String = "Built-in Sensor",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Milestone(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val description: String = "",
  val requiredSteps: Long = 0L,
  val category: String = "daily", // daily, streak, total, explorer
  val unlocked: Boolean = false,
  @ServerTimestamp val unlockedAt: Timestamp? = null,
  val icon: String = "directions_walk",
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LeaderboardEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val todaySteps: Long = 0L,
  val weeklySteps: Long = 0L,
  val streak: Long = 0L,
  val level: Long = 1L,
  val badge: String = "Pacesetter",
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudBackup(
  @DocumentId val id: String = "",
  val backupId: String = "",
  val userId: String = "",
  val backupName: String = "",
  val totalDaysLogged: Long = 0L,
  val totalStepsLogged: Long = 0L,
  val summaryJson: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

data class WearableDevice(
  val id: String,
  val name: String,
  val brand: String,
  val iconRes: String,
  val isConnected: Boolean,
  val batteryLevel: Int,
  val lastSyncTime: String,
  val liveHeartRateBpm: Int = 74,
  val liveCadenceSpm: Int = 112
)

data class WeeklyInsight(
  val title: String,
  val summary: String,
  val keyStat: String,
  val trendPercentage: Int, // e.g. +14% vs last week
  val isPositive: Boolean,
  val tip: String,
  val category: String // "Consistency", "Endurance", "Calories", "Recovery"
)
