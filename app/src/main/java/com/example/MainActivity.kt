package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserProfile
import com.example.data.repository.StepTrackerRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CloudBackupScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.MilestonesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WeeklyInsightsScreen
import com.example.ui.theme.StepTrackerTheme
import com.example.ui.viewmodel.StepTrackerViewModel
import com.example.ui.viewmodel.StepTrackerViewModelFactory
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

      DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
          currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
          Firebase.auth.removeAuthStateListener(listener)
        }
      }

      if (currentUser == null) {
        StepTrackerTheme {
          AuthScreen(
            onAuthSuccess = {
              currentUser = Firebase.auth.currentUser
            }
          )
        }
      } else {
        val uid = currentUser!!.uid
        key(uid) {
          MainAppContent(
            currentUserId = uid,
            onSignOut = {
              currentUser = null
            }
          )
        }
      }
    }
  }
}

@Composable
fun MainAppContent(
  currentUserId: String,
  onSignOut: () -> Unit
) {
  val context = LocalContext.current
  val repository = remember { StepTrackerRepository(context) }
  val viewModel: StepTrackerViewModel = viewModel(
    key = currentUserId,
    factory = StepTrackerViewModelFactory(repository, currentUserId, context)
  )

  val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
  val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
  val isWearableSyncing by viewModel.isWearableSyncing.collectAsStateWithLifecycle()
  val notificationGoalReminderEnabled by viewModel.notificationGoalReminderEnabled.collectAsStateWithLifecycle()

  val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
  val dailyLogsState by viewModel.dailyLogsState.collectAsStateWithLifecycle()
  val milestonesState by viewModel.milestonesState.collectAsStateWithLifecycle()
  val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
  val cloudBackupsState by viewModel.cloudBackupsState.collectAsStateWithLifecycle()
  val wearables by viewModel.wearables.collectAsStateWithLifecycle()

  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  var selectedTab by remember { mutableIntStateOf(0) }

  // Permission Launcher for Activity Recognition & Push Notifications
  val permissionsLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val notifGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true
    val activityGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: true
    } else true
    Log.d("Permissions", "Notifications: $notifGranted, Activity: $activityGranted")
  }

  LaunchedEffect(Unit) {
    val neededPermissions = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
        neededPermissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
      }
    }
    if (neededPermissions.isNotEmpty()) {
      permissionsLauncher.launch(neededPermissions.toTypedArray())
    }
  }

  LaunchedEffect(statusMessage) {
    statusMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  val profile = (userProfileState as? UiState.Success)?.data
  val dailyLogs = (dailyLogsState as? UiState.Success)?.data ?: emptyList()
  val milestones = (milestonesState as? UiState.Success)?.data ?: emptyList()
  val leaderboard = (leaderboardState as? UiState.Success)?.data ?: emptyList()
  val backups = (cloudBackupsState as? UiState.Success)?.data ?: emptyList()
  val insights = remember(dailyLogs) { viewModel.computeWeeklyInsights(dailyLogs) }

  StepTrackerTheme(themeMode = themeMode) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
        NavigationBar(
          modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("main_navigation_bar")
        ) {
          NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            icon = { Icon(Icons.Default.DirectionsWalk, contentDescription = "Dashboard") },
            label = { Text("Today") },
            modifier = Modifier.testTag("nav_tab_dashboard")
          )
          NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Insights") },
            label = { Text("Insights") },
            modifier = Modifier.testTag("nav_tab_insights")
          )
          NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Social") },
            label = { Text("Social") },
            modifier = Modifier.testTag("nav_tab_social")
          )
          NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { selectedTab = 3 },
            icon = { Icon(Icons.Default.MilitaryTech, contentDescription = "Milestones") },
            label = { Text("Badges") },
            modifier = Modifier.testTag("nav_tab_badges")
          )
          NavigationBarItem(
            selected = selectedTab == 4,
            onClick = { selectedTab = 4 },
            icon = { Icon(Icons.Default.CloudDone, contentDescription = "Cloud & Devices") },
            label = { Text("Cloud") },
            modifier = Modifier.testTag("nav_tab_cloud")
          )
          NavigationBarItem(
            selected = selectedTab == 5,
            onClick = { selectedTab = 5 },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            modifier = Modifier.testTag("nav_tab_settings")
          )
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        when (selectedTab) {
          0 -> DashboardScreen(
            userProfile = profile,
            dailyLogs = dailyLogs,
            wearables = wearables,
            isSyncing = isWearableSyncing,
            onAddSteps = { steps -> viewModel.addSteps(steps) },
            onSyncWearable = { device -> viewModel.syncWearableDevice(device) },
            onSendGoalReminder = { current, goal -> viewModel.triggerGoalReminderNotification(current, goal) }
          )

          1 -> WeeklyInsightsScreen(
            dailyLogs = dailyLogs,
            insights = insights
          )

          2 -> LeaderboardScreen(
            leaderboardEntries = leaderboard,
            currentUserId = currentUserId,
            onCheer = { /* Cheered */ }
          )

          3 -> MilestonesScreen(
            milestones = milestones,
            todaySteps = dailyLogs.firstOrNull()?.steps ?: 2450L,
            totalLifetimeSteps = profile?.totalSteps ?: 38400L
          )

          4 -> CloudBackupScreen(
            backups = backups,
            wearables = wearables,
            isSyncing = isWearableSyncing,
            onCreateBackup = { viewModel.createCloudBackup() },
            onRestoreBackup = { backup -> viewModel.restoreCloudBackup(backup) },
            onSyncWearable = { device -> viewModel.syncWearableDevice(device) }
          )

          5 -> SettingsScreen(
            userProfile = profile,
            currentThemeMode = themeMode,
            notificationReminderEnabled = notificationGoalReminderEnabled,
            onThemeModeChange = { mode -> viewModel.setThemeMode(mode) },
            onNotificationReminderChange = { enabled -> viewModel.setNotificationGoalReminderEnabled(enabled) },
            onUpdateDailyGoal = { newGoal ->
              viewModel.updateDailyGoal(newGoal)
            },
            onSendTestNotification = {
              viewModel.triggerGoalReminderNotification(
                dailyLogs.firstOrNull()?.steps ?: 2450L,
                profile?.dailyGoal ?: 10000L
              )
            },
            onSignOut = {
              performSignOut(context, coroutineScope, onSignOut)
            }
          )
        }
      }
    }
  }
}

fun performSignOut(
  context: Context,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit
) {
  val credentialManager = CredentialManager.create(context)
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("Auth", "Failed to clear credential state: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onSignOutComplete()
      }
    }
  }
}
