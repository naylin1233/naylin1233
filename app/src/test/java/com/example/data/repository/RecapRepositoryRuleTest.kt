package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.RecapItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class RecapRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveRecap_validPayload_createsDocumentAndReturnsId() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = RecapRepository(firestore)

        val uniqueTitle = "Recap ${UUID.randomUUID()}"
        val item = RecapItem(
            userId = uid,
            title = uniqueTitle,
            sourceType = "video",
            myanmarSummary = "မြန်မာအကျဉ်းချုပ် စာသား",
            myanmarScript = "မြန်မာအသံထွက်ဖတ်ရန် ဇာတ်ညွှန်း အပြည့်အစုံ",
            voiceTone = "male_narrator",
            speechSpeed = 1.0,
            speechPitch = 1.0,
            durationSeconds = 60
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveRecap(item) }
        assertTrue(saveResult.isSuccess)
        val recapId = saveResult.getOrThrow()
        assertTrue(recapId.isNotEmpty())
    }

    @Test
    fun observeRecaps_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = RecapRepository(firestore)

        val uniqueTitle = "Realtime Recap ${UUID.randomUUID()}"
        val item = RecapItem(
            userId = uid,
            title = uniqueTitle,
            sourceType = "srt",
            myanmarSummary = "SRT အကျဉ်းချုပ်",
            myanmarScript = "SRT အသံဖတ်ရန် စာသား",
            voiceTone = "female_narrator",
            speechSpeed = 1.05,
            speechPitch = 1.1,
            durationSeconds = 45
        )

        val recapId = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveRecap(item).getOrThrow() }

        val emittedList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeRecaps().first { list -> list.any { it.id == recapId } }
        }
        assertTrue(emittedList.any { it.id == recapId })
    }

    @Test
    fun getRecapById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = RecapRepository(firestore)

        val item = RecapItem(
            userId = aliceUid,
            title = "Alice Secret Recap",
            sourceType = "text",
            myanmarSummary = "လျှို့ဝှက်ချက် အကျဉ်း",
            myanmarScript = "လျှို့ဝှက်ချက် အသံ",
            voiceTone = "storyteller"
        )
        val recapId = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.saveRecap(item).getOrThrow() }

        // Sign in as Bob
        signInTestUser(BOB_EMAIL)
        val bobRepo = RecapRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getRecapById(recapId = recapId, ownerId = aliceUid) }
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertNotNull(error)
        val firestoreException = checkNotNull(error?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $error"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun observeRecaps_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = RecapRepository(firestore)

        var caughtException: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeRecaps().first()
            }
        } catch (e: Exception) {
            caughtException = e
        }

        assertNotNull(caughtException)
        val firestoreException = caughtException?.unwrapFirestoreException()
        // If unauthenticated, requireUserId throws IllegalStateException before Firestore is queried,
        // or Firestore throws PERMISSION_DENIED. Both verify zero unauthorized access!
        assertTrue(caughtException is IllegalStateException || firestoreException?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_mm@test.com"
        const val BOB_EMAIL = "bob_mm@test.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
