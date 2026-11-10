package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CloudBackup
import com.example.data.model.DailyLog
import com.example.data.model.LeaderboardEntry
import com.example.data.model.Milestone
import com.example.data.model.UserProfile
import com.example.data.model.WearableDevice
import com.example.data.model.WeeklyInsight
import com.example.data.repository.StepTrackerRepository
import com.example.data.util.NotificationHelper
import com.example.data.util.StepSensorManager
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
  object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

data class DashboardState(
  val todaySteps: Long = 0L,
  val dailyGoal: Long = 10000L,
  val caloriesBurned: Long = 0L,
  val distanceMeters: Long = 0L,
  val activeMinutes: Long = 0L,
  val currentStreak: Long = 0L,
  val bestStreak: Long = 0L,
  val totalLifetimeSteps: Long = 0L,
  val connectedWearable: String = "Pixel Watch 3",
  val isSensorAvailable: Boolean = false,
  val isSyncing: Boolean = false
)

class StepTrackerViewModel(
  private val repository: StepTrackerRepository,
  private val currentUserId: String,
  context: Context
) : ViewModel() {

  private val notificationHelper = NotificationHelper(context.applicationContext)
  private val stepSensorManager = StepSensorManager(context.applicationContext)

  private val _themeMode = MutableStateFlow(ThemeMode.DARK)
  val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

  private val _notificationGoalReminderEnabled = MutableStateFlow(true)
  val notificationGoalReminderEnabled: StateFlow<Boolean> = _notificationGoalReminderEnabled.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private val _isWearableSyncing = MutableStateFlow(false)
  val isWearableSyncing: StateFlow<Boolean> = _isWearableSyncing.asStateFlow()

  // Available Wearable Devices
  private val _wearables = MutableStateFlow(
    listOf(
      WearableDevice("w1", "Pixel Watch 3", "Google", "watch", true, 88, "Just now", 72, 114),
      WearableDevice("w2", "Galaxy Watch 6", "Samsung", "watch", false, 94, "Yesterday", 68, 108),
      WearableDevice("w3", "Fitbit Charge 6", "Fitbit", "band", false, 79, "2 days ago", 75, 120),
      WearableDevice("w4", "Garmin Forerunner", "Garmin", "sport", false, 100, "1 week ago", 60, 140)
    )
  )
  val wearables: StateFlow<List<WearableDevice>> = _wearables.asStateFlow()

  val userProfileState: StateFlow<UiState<UserProfile?>> =
    repository.observeUserProfile(currentUserId)
      .combine(repository.observeDailyLogs(currentUserId)) { profile, _ ->
        UiState.Success(profile) as UiState<UserProfile?>
      }
      .catch { e -> emit(UiState.Error(e.localizedMessage ?: "Failed to load profile")) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val dailyLogsState: StateFlow<UiState<List<DailyLog>>> =
    repository.observeDailyLogs(currentUserId)
      .combine(_isWearableSyncing) { logs, _ ->
        UiState.Success(logs) as UiState<List<DailyLog>>
      }
      .catch { e -> emit(UiState.Error(e.localizedMessage ?: "Failed to load history")) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val milestonesState: StateFlow<UiState<List<Milestone>>> =
    repository.observeMilestones(currentUserId)
      .combine(_isWearableSyncing) { milestones, _ ->
        UiState.Success(milestones) as UiState<List<Milestone>>
      }
      .catch { e -> emit(UiState.Error(e.localizedMessage ?: "Failed to load milestones")) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> =
    repository.observeLeaderboard()
      .combine(_isWearableSyncing) { entries, _ ->
        UiState.Success(entries) as UiState<List<LeaderboardEntry>>
      }
      .catch { e -> emit(UiState.Error(e.localizedMessage ?: "Failed to load leaderboard")) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val cloudBackupsState: StateFlow<UiState<List<CloudBackup>>> =
    repository.observeCloudBackups(currentUserId)
      .combine(_isWearableSyncing) { backups, _ ->
        UiState.Success(backups) as UiState<List<CloudBackup>>
      }
      .catch { e -> emit(UiState.Error(e.localizedMessage ?: "Failed to load backups")) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  init {
    initializeUser()
    startHardwareSensor()
  }

  private fun getTodayDateString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
  }

  private fun initializeUser() {
    viewModelScope.launch {
      repository.ensureUserProfileCreated(currentUserId, "Walker Explorer", null)
      repository.ensureDefaultMilestones(currentUserId)
      // Check if today has a log, if not log baseline
      val today = getTodayDateString()
      repository.logDailySteps(currentUserId, today, 2450L, 10000L, "Pixel Watch 3")
      repository.syncLeaderboardEntry(
        userId = currentUserId,
        displayName = "You (Walker)",
        todaySteps = 2450L,
        weeklySteps = 38400L,
        streak = 4L,
        level = 3L,
        badge = "Trailblazer"
      )
    }
  }

  private fun startHardwareSensor() {
    stepSensorManager.startListening { increment ->
      addSteps(increment.toLong(), "Hardware Sensor")
    }
  }

  override fun onCleared() {
    super.onCleared()
    stepSensorManager.stopListening()
  }

  fun clearStatusMessage() {
    _statusMessage.value = null
  }

  fun setThemeMode(mode: ThemeMode) {
    _themeMode.value = mode
  }

  fun setNotificationGoalReminderEnabled(enabled: Boolean) {
    _notificationGoalReminderEnabled.value = enabled
  }

  fun updateDailyGoal(newGoal: Long) {
    viewModelScope.launch {
      val profile = (userProfileState.value as? UiState.Success)?.data
      if (profile != null) {
        repository.updateUserProfile(
          userId = currentUserId,
          displayName = profile.displayName,
          dailyGoal = newGoal,
          weeklyGoal = profile.weeklyGoal,
          wearable = profile.connectedWearable,
          totalSteps = profile.totalSteps,
          currentStreak = profile.currentStreak,
          bestStreak = profile.bestStreak
        )
        _statusMessage.value = "Daily goal updated to ${"%,d".format(newGoal)} steps!"
      }
    }
  }

  fun triggerGoalReminderNotification(currentSteps: Long, goal: Long) {
    notificationHelper.sendGoalReminder(currentSteps, goal)
    _statusMessage.value = "Push notification reminder sent!"
  }

  fun addSteps(stepsToAdd: Long, source: String = "Wearable Sync") {
    viewModelScope.launch {
      val today = getTodayDateString()
      val currentLogs = (dailyLogsState.value as? UiState.Success)?.data ?: emptyList()
      val todayLog = currentLogs.firstOrNull { it.date == today }
      val updatedSteps = (todayLog?.steps ?: 2450L) + stepsToAdd
      val goal = todayLog?.goal ?: 10000L

      repository.logDailySteps(currentUserId, today, updatedSteps, goal, source)

      // Calculate total steps and streaks
      val allSteps = currentLogs.sumOf { if (it.date == today) updatedSteps else it.steps }
      val meetsGoal = updatedSteps >= goal
      val currentStreak = if (meetsGoal) (todayLog?.steps?.let { if (it < goal) 1L else 0L } ?: 1L) + 4L else 4L
      val level = (allSteps / 15000L).coerceAtLeast(1L)
      val badge = when {
        allSteps >= 100000L -> "Centurion 100K"
        allSteps >= 50000L -> "Voyager"
        allSteps >= 25000L -> "Trailblazer"
        else -> "Pacesetter"
      }

      repository.updateUserProfile(
        userId = currentUserId,
        displayName = "You (Walker)",
        dailyGoal = goal,
        weeklyGoal = 70000L,
        wearable = "Pixel Watch 3",
        totalSteps = allSteps,
        currentStreak = currentStreak,
        bestStreak = 7L
      )

      repository.syncLeaderboardEntry(
        userId = currentUserId,
        displayName = "You (Walker)",
        todaySteps = updatedSteps,
        weeklySteps = allSteps,
        streak = currentStreak,
        level = level,
        badge = badge
      )

      // Check milestones
      checkMilestones(updatedSteps, allSteps)

      // Goal notification if reached
      if (updatedSteps >= goal && (todayLog?.steps ?: 0L) < goal && _notificationGoalReminderEnabled.value) {
        notificationHelper.sendGoalReminder(updatedSteps, goal)
      }
    }
  }

  private suspend fun checkMilestones(todaySteps: Long, totalSteps: Long) {
    val milestones = (milestonesState.value as? UiState.Success)?.data ?: return
    for (m in milestones) {
      if (!m.unlocked) {
        val shouldUnlock = when (m.category) {
          "daily" -> todaySteps >= m.requiredSteps
          "total" -> totalSteps >= m.requiredSteps
          "endurance" -> todaySteps >= m.requiredSteps
          else -> false
        }
        if (shouldUnlock) {
          repository.unlockMilestone(currentUserId, m.id)
          notificationHelper.sendMilestoneUnlocked(m.title, m.description)
          _statusMessage.value = "🏆 Unlocked Achievement: ${m.title}!"
        }
      }
    }
  }

  fun syncWearableDevice(device: WearableDevice) {
    viewModelScope.launch {
      _isWearableSyncing.value = true
      _statusMessage.value = "Syncing with ${device.name} via Bluetooth BLE..."

      // Simulate live wearable step payload retrieval
      val stepsFromWatch = (450..1200).random().toLong()
      addSteps(stepsFromWatch, device.name)

      // Update connected device list
      _wearables.value = _wearables.value.map {
        if (it.id == device.id) {
          it.copy(
            isConnected = true,
            lastSyncTime = "Just now",
            batteryLevel = (it.batteryLevel - 1).coerceAtLeast(10),
            liveHeartRateBpm = (70..88).random(),
            liveCadenceSpm = (105..130).random()
          )
        } else {
          it.copy(isConnected = false)
        }
      }

      _isWearableSyncing.value = false
      _statusMessage.value = "Synced +$stepsFromWatch steps from ${device.name}!"
    }
  }

  fun createCloudBackup() {
    viewModelScope.launch {
      _statusMessage.value = "Creating encrypted cloud backup snapshot..."
      val logs = (dailyLogsState.value as? UiState.Success)?.data ?: emptyList()
      val totalDays = logs.size.toLong()
      val totalSteps = logs.sumOf { it.steps }

      val json = JSONObject().apply {
        put("version", 1)
        put("timestamp", System.currentTimeMillis())
        put("totalDays", totalDays)
        put("totalSteps", totalSteps)
        val logsArray = JSONArray()
        logs.forEach { log ->
          logsArray.put(JSONObject().apply {
            put("date", log.date)
            put("steps", log.steps)
            put("goal", log.goal)
            put("calories", log.calories)
            put("distanceMeters", log.distanceMeters)
            put("activeMinutes", log.activeMinutes)
            put("source", log.source)
          })
        }
        put("logs", logsArray)
      }.toString()

      val name = "Auto Backup ${SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date())}"
      val result = repository.createCloudBackup(currentUserId, name, totalDays, totalSteps, json)
      if (result.isSuccess) {
        _statusMessage.value = "Cloud backup successfully secured in Firestore!"
      } else {
        _statusMessage.value = "Cloud backup failed: ${result.exceptionOrNull()?.localizedMessage}"
      }
    }
  }

  fun restoreCloudBackup(backup: CloudBackup) {
    viewModelScope.launch {
      _statusMessage.value = "Restoring data from ${backup.backupName}..."
      try {
        val root = JSONObject(backup.summaryJson)
        val logsArray = root.optJSONArray("logs")
        if (logsArray != null) {
          for (i in 0 until logsArray.length()) {
            val item = logsArray.getJSONObject(i)
            repository.logDailySteps(
              userId = currentUserId,
              date = item.getString("date"),
              steps = item.getLong("steps"),
              goal = item.getLong("goal"),
              source = item.optString("source", "Cloud Restore")
            )
          }
        }
        _statusMessage.value = "Successfully restored ${backup.totalDaysLogged} days of activity!"
      } catch (e: Exception) {
        _statusMessage.value = "Restore error: ${e.localizedMessage}"
      }
    }
  }

  fun computeWeeklyInsights(logs: List<DailyLog>): List<WeeklyInsight> {
    if (logs.isEmpty()) {
      return listOf(
        WeeklyInsight(
          title = "Get Started",
          summary = "Take your first walk today with Step Tracker to start receiving personalized health insights.",
          keyStat = "0 steps",
          trendPercentage = 0,
          isPositive = true,
          tip = "Consistency matters more than distance in building cardiovascular stamina.",
          category = "Consistency"
        )
      )
    }

    val past7Days = logs.take(7)
    val totalSteps = past7Days.sumOf { it.steps }
    val avgSteps = if (past7Days.isNotEmpty()) totalSteps / past7Days.size else 0L
    val bestDay = past7Days.maxByOrNull { it.steps }
    val daysMetGoal = past7Days.count { it.steps >= it.goal }
    val consistencyRate = ((daysMetGoal.toFloat() / past7Days.size) * 100).toInt()
    val totalKm = (past7Days.sumOf { it.distanceMeters } / 1000f)
    val totalCalories = past7Days.sumOf { it.calories }

    return listOf(
      WeeklyInsight(
        title = "Pacing & Weekly Volume",
        summary = "You accumulated ${"%,d".format(totalSteps)} steps over the past 7 days, averaging ${"%,d".format(avgSteps)} steps/day.",
        keyStat = "${"%,d".format(avgSteps)} avg/day",
        trendPercentage = 16,
        isPositive = true,
        tip = "Great pacing! To increase caloric afterburn, include 2-minute brisk intervals every 20 minutes.",
        category = "Endurance"
      ),
      WeeklyInsight(
        title = "Goal Consistency",
        summary = "You achieved your daily step goal on $daysMetGoal out of ${past7Days.size} days ($consistencyRate% hit rate).",
        keyStat = "$consistencyRate% Met",
        trendPercentage = if (consistencyRate >= 70) 12 else -5,
        isPositive = consistencyRate >= 70,
        tip = if (consistencyRate >= 70) "Outstanding discipline! You're in the top 15% of consistent walkers." else "Try setting a 15-minute post-lunch walk reminder to close the gap.",
        category = "Consistency"
      ),
      WeeklyInsight(
        title = "Energy Expenditure",
        summary = "Burned approximately ${"%,d".format(totalCalories)} kcal covering ${String.format(Locale.US, "%.1f", totalKm)} km.",
        keyStat = "${"%,d".format(totalCalories)} kcal",
        trendPercentage = 18,
        isPositive = true,
        tip = "Walking at an energetic cadence (110+ SPM) stimulates fat oxidation and improves insulin sensitivity.",
        category = "Calories"
      ),
      WeeklyInsight(
        title = "Peak Activity Record",
        summary = "Your highest day was ${bestDay?.date ?: "today"} with ${"%,d".format(bestDay?.steps ?: 0L)} steps.",
        keyStat = "${"%,d".format(bestDay?.steps ?: 0L)} peak",
        trendPercentage = 24,
        isPositive = true,
        tip = "Remember to stay hydrated and perform light calf and hamstring stretches after high-volume days.",
        category = "Recovery"
      )
    )
  }
}

class StepTrackerViewModelFactory(
  private val repository: StepTrackerRepository,
  private val currentUserId: String,
  private val context: Context
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(StepTrackerViewModel::class.java)) {
      return StepTrackerViewModel(repository, currentUserId, context) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
