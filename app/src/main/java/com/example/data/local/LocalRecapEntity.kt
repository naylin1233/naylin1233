package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.RecapItem

@Entity(tableName = "local_recaps")
data class LocalRecapEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val sourceType: String,
    val sourceUrl: String?,
    val originalText: String?,
    val myanmarSummary: String,
    val myanmarScript: String,
    val subtitlesSrt: String?,
    val voiceTone: String,
    val speechSpeed: Double,
    val speechPitch: Double,
    val durationSeconds: Int,
    val timestamp: Long
) {
    fun toRecapItem(): RecapItem {
        return RecapItem(
            id = id,
            userId = userId,
            title = title,
            sourceType = sourceType,
            sourceUrl = sourceUrl,
            originalText = originalText,
            myanmarSummary = myanmarSummary,
            myanmarScript = myanmarScript,
            subtitlesSrt = subtitlesSrt,
            voiceTone = voiceTone,
            speechSpeed = speechSpeed,
            speechPitch = speechPitch,
            durationSeconds = durationSeconds
        )
    }

    companion object {
        fun fromRecapItem(item: RecapItem): LocalRecapEntity {
            return LocalRecapEntity(
                id = item.id,
                userId = item.userId,
                title = item.title,
                sourceType = item.sourceType,
                sourceUrl = item.sourceUrl,
                originalText = item.originalText,
                myanmarSummary = item.myanmarSummary,
                myanmarScript = item.myanmarScript,
                subtitlesSrt = item.subtitlesSrt,
                voiceTone = item.voiceTone,
                speechSpeed = item.speechSpeed,
                speechPitch = item.speechPitch,
                durationSeconds = item.durationSeconds,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
