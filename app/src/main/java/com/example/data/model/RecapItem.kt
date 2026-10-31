package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class RecapItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val sourceType: String = "video", // video, srt, text, youtube, tiktok
    val sourceUrl: String? = null,
    val originalText: String? = null,
    val myanmarSummary: String = "",
    val myanmarScript: String = "",
    val subtitlesSrt: String? = null,
    val voiceTone: String = "male_narrator",
    val speechSpeed: Double = 1.0,
    val speechPitch: Double = 1.0,
    val durationSeconds: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        map["userId"] = userId
        map["title"] = title
        map["sourceType"] = sourceType
        sourceUrl?.let { map["sourceUrl"] = it }
        originalText?.let { map["originalText"] = it }
        map["myanmarSummary"] = myanmarSummary
        map["myanmarScript"] = myanmarScript
        subtitlesSrt?.let { map["subtitlesSrt"] = it }
        map["voiceTone"] = voiceTone
        map["speechSpeed"] = speechSpeed
        map["speechPitch"] = speechPitch
        map["durationSeconds"] = durationSeconds
        map["createdAt"] = createdAt ?: FieldValue.serverTimestamp()
        map["updatedAt"] = FieldValue.serverTimestamp()
        return map.filterValues { it != null }
    }
}

enum class VoiceProfile(
    val id: String,
    val displayNameMm: String,
    val displayNameEn: String,
    val pitch: Float,
    val speed: Float
) {
    MALE_NARRATOR("male_narrator", "အမျိုးသား အသံဆွေးနွေးသူ (Narrator)", "Male Narrator", 0.95f, 1.0f),
    FEMALE_NARRATOR("female_narrator", "အမျိုးသမီး အသံ (Female Voice)", "Female Narrator", 1.15f, 1.05f),
    STORYTELLER("storyteller", "ဇာတ်လမ်းပြောသူ (Storyteller)", "Storyteller", 0.85f, 0.95f),
    ENERGETIC("energetic", "တက်ကြွသောအသံ (Energetic)", "Energetic", 1.25f, 1.2f),
    CALM("calm", "ငြိမ်သက်အေးဆေးသောအသံ (Calm)", "Calm", 0.9f, 0.9f),
    NEWS_ANCHOR("news_anchor", "သတင်းကြေညာသူ (News Anchor)", "News Anchor", 1.0f, 1.1f);

    companion object {
        fun fromId(id: String): VoiceProfile {
            return entries.find { it.id == id } ?: MALE_NARRATOR
        }
    }
}
