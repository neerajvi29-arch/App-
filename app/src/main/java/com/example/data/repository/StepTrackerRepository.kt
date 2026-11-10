package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CloudBackup
import com.example.data.model.DailyLog
import com.example.data.model.LeaderboardEntry
import com.example.data.model.Milestone
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StepTrackerRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val path = "users/$userId"
    val docRef = db.collection("users").document(userId)
    val flow = docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.GET, path)
        }
        throw error
      }
    emitAll(flow)
  }

  suspend fun ensureUserProfileCreated(
    userId: String,
    displayName: String,
    email: String?
  ): Result<Unit> {
    val docRef = db.collection("users").document(userId)
    return try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        val payload = mutableMapOf<String, Any>(
          "userId" to userId,
          "displayName" to displayName.ifBlank { "Step Explorer" },
          "dailyGoal" to 10000L,
          "weeklyGoal" to 70000L,
          "totalSteps" to 0L,
          "currentStreak" to 0L,
          "bestStreak" to 0L,
          "connectedWearable" to "Pixel Watch 3",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        if (email != null) {
          payload["email"] = email
        }
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateUserProfile(
    userId: String,
    displayName: String,
    dailyGoal: Long,
    weeklyGoal: Long,
    wearable: String,
    totalSteps: Long,
    currentStreak: Long,
    bestStreak: Long
  ): Result<Unit> {
    val docRef = db.collection("users").document(userId)
    return try {
      val updates = mapOf(
        "displayName" to displayName,
        "dailyGoal" to dailyGoal,
        "weeklyGoal" to weeklyGoal,
        "connectedWearable" to wearable,
        "totalSteps" to totalSteps,
        "currentStreak" to currentStreak,
        "bestStreak" to bestStreak,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeDailyLogs(userId: String): Flow<List<DailyLog>> = flow {
    val path = "users/$userId/daily_logs"
    val query = db.collection("users").document(userId).collection("daily_logs")
      .orderBy("date", Query.Direction.DESCENDING)
      .limit(30)

    val flow = query.snapshots()
      .map { snapshot -> snapshot.toObjects(DailyLog::class.java) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
    emitAll(flow)
  }

  suspend fun logDailySteps(
    userId: String,
    date: String,
    steps: Long,
    goal: Long,
    source: String = "Built-in Sensor"
  ): Result<Unit> {
    val docRef = db.collection("users").document(userId).collection("daily_logs").document(date)
    return try {
      val snapshot = docRef.get().await()
      // Estimate metrics: ~0.762m per step, ~0.04 kcal per step
      val distanceMeters = (steps * 0.762).toLong()
      val calories = (steps * 0.04).toLong()
      val activeMinutes = (steps / 100).coerceAtLeast(1)

      if (!snapshot.exists()) {
        val payload = mapOf(
          "date" to date,
          "userId" to userId,
          "steps" to steps,
          "goal" to goal,
          "calories" to calories,
          "distanceMeters" to distanceMeters,
          "activeMinutes" to activeMinutes,
          "source" to source,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
      } else {
        val updates = mapOf(
          "steps" to steps,
          "goal" to goal,
          "calories" to calories,
          "distanceMeters" to distanceMeters,
          "activeMinutes" to activeMinutes,
          "source" to source,
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(updates).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeMilestones(userId: String): Flow<List<Milestone>> = flow {
    val path = "users/$userId/milestones"
    val query = db.collection("users").document(userId).collection("milestones")
      .orderBy("requiredSteps", Query.Direction.ASCENDING)

    val flow = query.snapshots()
      .map { snapshot -> snapshot.toObjects(Milestone::class.java) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
    emitAll(flow)
  }

  suspend fun ensureDefaultMilestones(userId: String): Result<Unit> {
    val collectionRef = db.collection("users").document(userId).collection("milestones")
    return try {
      val existing = collectionRef.get().await()
      if (existing.isEmpty) {
        val defaultList = listOf(
          Milestone(id = "m_first_5k", userId = userId, title = "First 5,000 Steps", description = "Reach 5,000 steps in a single day", requiredSteps = 5000L, category = "daily", icon = "directions_walk"),
          Milestone(id = "m_10k_club", userId = userId, title = "10K Champion", description = "Hit the golden 10,000 steps milestone", requiredSteps = 10000L, category = "daily", icon = "emoji_events"),
          Milestone(id = "m_15k_power", userId = userId, title = "Power Strider", description = "Smash 15,000 steps in 24 hours", requiredSteps = 15000L, category = "daily", icon = "bolt"),
          Milestone(id = "m_marathon_steps", userId = userId, title = "Half-Marathon Day", description = "Hit 25,000 steps in a single day", requiredSteps = 25000L, category = "endurance", icon = "military_tech"),
          Milestone(id = "m_50k_week", userId = userId, title = "50K Week Voyager", description = "Accumulate 50,000 total steps across weekly logs", requiredSteps = 50000L, category = "total", icon = "workspace_premium"),
          Milestone(id = "m_100k_club", userId = userId, title = "Centurion 100K", description = "Reach 100,000 lifetime steps in cloud history", requiredSteps = 100000L, category = "total", icon = "diamond")
        )

        for (m in defaultList) {
          val payload = mapOf(
            "id" to m.id,
            "userId" to userId,
            "title" to m.title,
            "description" to m.description,
            "requiredSteps" to m.requiredSteps,
            "category" to m.category,
            "unlocked" to false,
            "icon" to m.icon,
            "updatedAt" to FieldValue.serverTimestamp()
          )
          collectionRef.document(m.id).set(payload).await()
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, collectionRef.path)
      Result.failure(e)
    }
  }

  suspend fun unlockMilestone(userId: String, milestoneId: String): Result<Unit> {
    val docRef = db.collection("users").document(userId).collection("milestones").document(milestoneId)
    return try {
      val updates = mapOf(
        "unlocked" to true,
        "unlockedAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = flow {
    val path = "leaderboard"
    val query = db.collection(path)
      .orderBy("todaySteps", Query.Direction.DESCENDING)
      .limit(50)

    val flow = query.snapshots()
      .map { snapshot -> snapshot.toObjects(LeaderboardEntry::class.java) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
    emitAll(flow)
  }

  suspend fun syncLeaderboardEntry(
    userId: String,
    displayName: String,
    todaySteps: Long,
    weeklySteps: Long,
    streak: Long,
    level: Long,
    badge: String
  ): Result<Unit> {
    val docRef = db.collection("leaderboard").document(userId)
    return try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        val payload = mapOf(
          "userId" to userId,
          "displayName" to displayName.ifBlank { "Walker #$streak" },
          "todaySteps" to todaySteps,
          "weeklySteps" to weeklySteps,
          "streak" to streak,
          "level" to level,
          "badge" to badge,
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
      } else {
        val updates = mapOf(
          "displayName" to displayName.ifBlank { "Walker #$streak" },
          "todaySteps" to todaySteps,
          "weeklySteps" to weeklySteps,
          "streak" to streak,
          "level" to level,
          "badge" to badge,
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(updates).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeCloudBackups(userId: String): Flow<List<CloudBackup>> = flow {
    val path = "users/$userId/backups"
    val query = db.collection("users").document(userId).collection("backups")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .limit(20)

    val flow = query.snapshots()
      .map { snapshot -> snapshot.toObjects(CloudBackup::class.java) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
    emitAll(flow)
  }

  suspend fun createCloudBackup(
    userId: String,
    backupName: String,
    totalDays: Long,
    totalSteps: Long,
    summaryJson: String
  ): Result<String> {
    val backupId = "backup_" + UUID.randomUUID().toString().take(8)
    val docRef = db.collection("users").document(userId).collection("backups").document(backupId)
    return try {
      val payload = mapOf(
        "backupId" to backupId,
        "userId" to userId,
        "backupName" to backupName,
        "totalDaysLogged" to totalDays,
        "totalStepsLogged" to totalSteps,
        "summaryJson" to summaryJson,
        "createdAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()
      Result.success(backupId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

}
