package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeaderboardEntry

@Composable
fun LeaderboardScreen(
  leaderboardEntries: List<LeaderboardEntry>,
  currentUserId: String,
  onCheer: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Today, 1 = Weekly
  var cheerFeedback by remember { mutableStateOf<String?>(null) }

  // Ensure there are friendly competitors in the leaderboard for rich competition
  val populatedEntries = remember(leaderboardEntries) {
    val existing = leaderboardEntries.toMutableList()
    val friendMocks = listOf(
      LeaderboardEntry(id = "m1", userId = "u_sarah", displayName = "Sarah Jenkins", todaySteps = 12450L, weeklySteps = 78200L, streak = 9L, level = 6L, badge = "Marathoner"),
      LeaderboardEntry(id = "m2", userId = "u_marcus", displayName = "Marcus Vance", todaySteps = 10180L, weeklySteps = 71500L, streak = 6L, level = 5L, badge = "Trailblazer"),
      LeaderboardEntry(id = "m3", userId = "u_priya", displayName = "Priya Sharma", todaySteps = 8920L, weeklySteps = 64300L, streak = 5L, level = 4L, badge = "Pacesetter"),
      LeaderboardEntry(id = "m4", userId = "u_david", displayName = "David Kim", todaySteps = 6400L, weeklySteps = 45000L, streak = 3L, level = 3L, badge = "Strider"),
      LeaderboardEntry(id = "m5", userId = "u_elena", displayName = "Elena Rostova", todaySteps = 5120L, weeklySteps = 39000L, streak = 2L, level = 2L, badge = "Explorer")
    )
    for (friend in friendMocks) {
      if (existing.none { it.userId == friend.userId }) {
        existing.add(friend)
      }
    }
    existing
  }

  val sortedEntries = remember(populatedEntries, selectedTab) {
    if (selectedTab == 0) {
      populatedEntries.sortedByDescending { it.todaySteps }
    } else {
      populatedEntries.sortedByDescending { it.weeklySteps }
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
      .testTag("leaderboard_screen"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.EmojiEvents,
          contentDescription = null,
          tint = Color(0xFFF59E0B),
          modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Social Leaderboard",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
      Text(
        text = "Daily competition and friendly challenges with step partners",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
      )

      // Timeframe Tab Row
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("Today's Race", fontWeight = FontWeight.Bold) }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Weekly League", fontWeight = FontWeight.Bold) }
        )
      }
    }

    if (cheerFeedback != null) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(8.dp)
        ) {
          Text(
            text = cheerFeedback!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }

    // Top 3 Podium
    if (sortedEntries.size >= 3) {
      item {
        PodiumCard(
          first = sortedEntries[0],
          second = sortedEntries[1],
          third = sortedEntries[2],
          isWeekly = selectedTab == 1
        )
      }
    }

    item {
      Text(
        text = "All Competitors",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 6.dp)
      )
    }

    itemsIndexed(sortedEntries) { index, entry ->
      val isCurrentUser = entry.userId == currentUserId
      LeaderboardRow(
        rank = index + 1,
        entry = entry,
        isCurrentUser = isCurrentUser,
        isWeekly = selectedTab == 1,
        onCheer = {
          cheerFeedback = "🎉 Cheered ${entry.displayName}! High five sent."
          onCheer(entry.displayName)
        }
      )
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun PodiumCard(
  first: LeaderboardEntry,
  second: LeaderboardEntry,
  third: LeaderboardEntry,
  isWeekly: Boolean
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(20.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "🏆 Podium Leaders",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Bottom
      ) {
        // #2 Silver
        PodiumPillar(entry = second, rank = 2, color = Color(0xFF94A3B8), isWeekly = isWeekly)
        // #1 Gold
        PodiumPillar(entry = first, rank = 1, color = Color(0xFFF59E0B), isWeekly = isWeekly)
        // #3 Bronze
        PodiumPillar(entry = third, rank = 3, color = Color(0xFFB45309), isWeekly = isWeekly)
      }
    }
  }
}

@Composable
private fun PodiumPillar(
  entry: LeaderboardEntry,
  rank: Int,
  color: Color,
  isWeekly: Boolean
) {
  val height = when (rank) {
    1 -> 120.dp
    2 -> 96.dp
    else -> 80.dp
  }
  val steps = if (isWeekly) entry.weeklySteps else entry.todaySteps

  Column(
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = entry.displayName.take(10),
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = "${"%,d".format(steps)}",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(6.dp))

    Box(
      modifier = Modifier
        .width(84.dp)
        .height(height)
        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        .background(color.copy(alpha = 0.2f)),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "#$rank",
          fontSize = 24.sp,
          fontWeight = FontWeight.ExtraBold,
          color = color
        )
      }
    }
  }
}

@Composable
private fun LeaderboardRow(
  rank: Int,
  entry: LeaderboardEntry,
  isCurrentUser: Boolean,
  isWeekly: Boolean,
  onCheer: () -> Unit
) {
  val steps = if (isWeekly) entry.weeklySteps else entry.todaySteps
  val containerColor = if (isCurrentUser) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
  } else {
    MaterialTheme.colorScheme.surface
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    shape = RoundedCornerShape(14.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "#$rank",
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = if (rank <= 3) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(32.dp)
      )

      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = entry.displayName.take(1).uppercase(),
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = if (isCurrentUser) "${entry.displayName} (You)" else entry.displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
          )
          if (entry.streak > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Streak",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = "${entry.streak}d",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444)
              )
            }
          }
        }
        Text(
          text = "${entry.badge} • Lv.${entry.level}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "${"%,d".format(steps)}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = if (isWeekly) "weekly steps" else "steps today",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      if (!isCurrentUser) {
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = onCheer,
          modifier = Modifier.size(36.dp),
          shape = CircleShape,
          contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
          )
        ) {
          Icon(
            imageVector = Icons.Default.ThumbUp,
            contentDescription = "Cheer",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}
