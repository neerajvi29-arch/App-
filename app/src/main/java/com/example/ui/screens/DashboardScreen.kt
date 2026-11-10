package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyLog
import com.example.data.model.UserProfile
import com.example.data.model.WearableDevice
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
  userProfile: UserProfile?,
  dailyLogs: List<DailyLog>,
  wearables: List<WearableDevice>,
  isSyncing: Boolean,
  onAddSteps: (Long) -> Unit,
  onSyncWearable: (WearableDevice) -> Unit,
  onSendGoalReminder: (Long, Long) -> Unit,
  modifier: Modifier = Modifier
) {
  val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
  val todayLog = dailyLogs.firstOrNull { it.date == todayString }
  val currentSteps = todayLog?.steps ?: 2450L
  val goal = todayLog?.goal ?: userProfile?.dailyGoal ?: 10000L
  val calories = todayLog?.calories ?: (currentSteps * 0.04).toLong()
  val distanceKm = (todayLog?.distanceMeters ?: (currentSteps * 0.762).toLong()) / 1000f
  val activeMins = todayLog?.activeMinutes ?: (currentSteps / 100).coerceAtLeast(1)
  val connectedWatch = wearables.firstOrNull { it.isConnected } ?: wearables.first()

  val progress = (currentSteps.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
  val animatedProgress by animateFloatAsState(
    targetValue = progress,
    animationSpec = tween(durationMillis = 800),
    label = "step_ring_progress"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
      .testTag("dashboard_screen"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Header Greeting
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Welcome back,",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = userProfile?.displayName ?: "Walker",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
      }

      // Sync trigger badge
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .clickable { onSyncWearable(connectedWatch) }
          .padding(horizontal = 12.dp, vertical = 6.dp)
          .testTag("quick_sync_badge"),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isSyncing) {
            CircularProgressIndicator(
              modifier = Modifier.size(14.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary
            )
          } else {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = "Sync",
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.primary
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isSyncing) "Syncing..." else "Sync Wearable",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Circular Step Progress Ring
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("step_ring_card"),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
      shape = RoundedCornerShape(24.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier.size(220.dp),
          contentAlignment = Alignment.Center
        ) {
          val ringTrackColor = MaterialTheme.colorScheme.surfaceVariant
          val primaryColor = MaterialTheme.colorScheme.primary
          val secondaryColor = MaterialTheme.colorScheme.secondary

          Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // Background Track
            drawArc(
              color = ringTrackColor,
              startAngle = 135f,
              sweepAngle = 270f,
              useCenter = false,
              topLeft = topLeft,
              size = arcSize,
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Foreground Progress Arc with Gradient
            val gradientBrush = Brush.sweepGradient(
              listOf(primaryColor, secondaryColor, primaryColor)
            )
            drawArc(
              brush = gradientBrush,
              startAngle = 135f,
              sweepAngle = 270f * animatedProgress,
              useCenter = false,
              topLeft = topLeft,
              size = arcSize,
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.DirectionsWalk,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp)
            )
            Text(
              text = "%,d".format(currentSteps),
              fontSize = 38.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Goal: ${"%,d".format(goal)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "${(progress * 100).toInt()}% completed",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.padding(top = 2.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 3 Metrics Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      MetricCard(
        modifier = Modifier.weight(1f),
        icon = Icons.Default.DirectionsRun,
        label = "Distance",
        value = String.format(Locale.US, "%.2f", distanceKm),
        unit = "km",
        tint = MaterialTheme.colorScheme.secondary
      )
      MetricCard(
        modifier = Modifier.weight(1f),
        icon = Icons.Default.LocalFireDepartment,
        label = "Calories",
        value = "%,d".format(calories),
        unit = "kcal",
        tint = MaterialTheme.colorScheme.tertiary
      )
      MetricCard(
        modifier = Modifier.weight(1f),
        icon = Icons.Default.Timer,
        label = "Active",
        value = activeMins.toString(),
        unit = "mins",
        tint = MaterialTheme.colorScheme.primary
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Connected Wearable Status Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("wearable_status_card"),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      ),
      shape = RoundedCornerShape(18.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Watch,
                contentDescription = "Wearable",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = connectedWatch.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Synced: ${connectedWatch.lastSyncTime}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.BatteryChargingFull,
              contentDescription = "Battery",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${connectedWatch.batteryLevel}%",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Favorite,
              contentDescription = "Heart Rate",
              tint = Color(0xFFEF4444),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${connectedWatch.liveHeartRateBpm} BPM (Live)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.DirectionsRun,
              contentDescription = "Cadence",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${connectedWatch.liveCadenceSpm} SPM Cadence",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Past 7 Days Activity Mini-Chart
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("weekly_minichart_card"),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(18.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Weekly Activity Pulse",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Last 7 recorded sessions",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        val pastWeekLogs = dailyLogs.take(7).reversed()
        val maxSteps = (pastWeekLogs.maxOfOrNull { it.steps } ?: 10000L).coerceAtLeast(10000L)

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom
        ) {
          if (pastWeekLogs.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "No history yet. Start walking to see trends!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            pastWeekLogs.forEach { log ->
              val barRatio = (log.steps.toFloat() / maxSteps.toFloat()).coerceIn(0.1f, 1f)
              val isToday = log.date == todayString
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
              ) {
                Box(
                  modifier = Modifier
                    .width(22.dp)
                    .height((barRatio * 90).dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(
                      if (isToday) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = log.date.takeLast(2),
                  fontSize = 10.sp,
                  color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Interactive Action Controls
    Text(
      text = "Quick Step Simulator & Test Actions",
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.align(Alignment.Start)
    )

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = { onAddSteps(500L) },
        modifier = Modifier
          .weight(1f)
          .testTag("add_500_steps_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
      ) {
        Text("+500 Steps", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      }

      Button(
        onClick = { onAddSteps(2000L) },
        modifier = Modifier
          .weight(1f)
          .testTag("add_2000_steps_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
      ) {
        Text("+2,000 Steps", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedButton(
      onClick = { onSendGoalReminder(currentSteps, goal) },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("test_push_notification_button"),
      shape = RoundedCornerShape(12.dp)
    ) {
      Icon(
        imageVector = Icons.Default.NotificationsActive,
        contentDescription = "Notification",
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text("Send Goal Reminder Push Notification", fontSize = 13.sp)
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

@Composable
private fun MetricCard(
  modifier: Modifier = Modifier,
  icon: ImageVector,
  label: String,
  value: String,
  unit: String,
  tint: Color
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      horizontalAlignment = Alignment.Start
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = tint,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = unit,
          style = MaterialTheme.typography.bodySmall,
          color = tint,
          fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
