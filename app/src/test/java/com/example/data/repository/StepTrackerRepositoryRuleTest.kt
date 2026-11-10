package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StepTrackerRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureUserProfileCreated_authenticatedUser_createsProfile(): Unit = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = StepTrackerRepository(firestore)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureUserProfileCreated(aliceUid, "Alice Walker", ALICE_EMAIL)
    }
    assertTrue(result.isSuccess)

    val profile = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeUserProfile(aliceUid).first { it != null }
    }
    assertNotNull(profile)
    assertEquals(aliceUid, profile?.userId)
    assertEquals("Alice Walker", profile?.displayName)
  }

  @Test
  fun logDailySteps_authenticatedUser_updatesLog(): Unit = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = StepTrackerRepository(firestore)

    val logResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.logDailySteps(aliceUid, "2026-11-10", 7500L, 10000L, "Pixel Watch 3")
    }
    assertTrue(logResult.isSuccess)

    val logs = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeDailyLogs(aliceUid).first { it.isNotEmpty() }
    }
    assertEquals(7500L, logs.first().steps)
    assertEquals("Pixel Watch 3", logs.first().source)
  }

  @Test
  fun leaderboard_authenticatedUser_syncsAndReads(): Unit = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = StepTrackerRepository(firestore)

    val syncResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.syncLeaderboardEntry(aliceUid, "Alice Champion", 9400L, 62000L, 6L, 5L, "Pacesetter")
    }
    assertTrue(syncResult.isSuccess)

    val entries = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeLeaderboard().first { list -> list.any { it.userId == aliceUid } }
    }
    assertTrue(entries.any { it.userId == aliceUid && it.todaySteps == 9400L })
  }

  @Test
  fun observeUserProfile_unauthenticatedUser_failsWithPermissionDenied(): Unit = runBlocking {
    val aliceUid = "unauth_target_123"
    auth.signOut()
    val repo = StepTrackerRepository(firestore)

    val exception = assertThrows(Exception::class.java) {
      runBlocking {
        withTimeout(FLOW_TIMEOUT_MS) {
          repo.observeUserProfile(aliceUid).first()
        }
      }
    }
    val firestoreException = checkNotNull(exception.unwrapFirestoreException()) {
      "Expected FirebaseFirestoreException, got $exception"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@steptracker.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
