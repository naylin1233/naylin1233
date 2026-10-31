package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.RecapItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RecapRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeRecaps(): Flow<List<RecapItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/recaps"
        emitAll(
            db.collection("users").document(uid).collection("recaps")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(RecapItem::class.java) }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getRecapById(recapId: String, ownerId: String = requireUserId()): Result<RecapItem> {
        val path = "users/$ownerId/recaps/$recapId"
        return try {
            val snapshot = db.collection("users").document(ownerId).collection("recaps").document(recapId).get().await()
            val item = snapshot.toObject(RecapItem::class.java)
            if (item != null) {
                Result.success(item)
            } else {
                Result.failure(NoSuchElementException("Recap document not found"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun saveRecap(item: RecapItem): Result<String> {
        val uid = requireUserId()
        val recapId = if (item.id.isNotBlank()) item.id else UUID.randomUUID().toString()
        val path = "users/$uid/recaps/$recapId"
        val docRef = db.collection("users").document(uid).collection("recaps").document(recapId)
        val payload = item.copy(id = recapId, userId = uid).toMap()

        return try {
            docRef.set(payload).await()
            Result.success(recapId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteRecap(recapId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/recaps/$recapId"
        return try {
            db.collection("users").document(uid).collection("recaps").document(recapId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
