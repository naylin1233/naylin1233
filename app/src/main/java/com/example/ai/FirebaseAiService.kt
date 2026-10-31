package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

data class RecapResult(
    val title: String,
    val summary: String,
    val voiceScript: String,
    val keyPoints: List<String>,
    val srtSubtitles: String
)

class FirebaseAiService {

    companion object {
        private const val TAG = "FirebaseAiService"
        private const val TIMEOUT_MS = 60_000L
    }

    // Lazy initialization mandated by Firebase AI Logic
    private val flashModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = com.google.firebase.ai.type.content {
                text(
                    "You are an expert Myanmar (Burmese) video recap, translator and voiceover script specialist. " +
                    "Your job is to generate natural, engaging, grammatically correct and fluent Myanmar spoken scripts (အပြောစကားပြေ) " +
                    "and accurate Myanmar translations that sound compelling when read aloud by Text-to-Speech engines. " +
                    "Always prioritize natural Burmese phrasing over literal word-by-word translation."
                )
            }
        )
    }

    private val liteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.1-flash-lite",
            systemInstruction = com.google.firebase.ai.type.content {
                text("You are a high-speed translator providing rapid, accurate Burmese (Myanmar) translations.")
            }
        )
    }

    private val recapSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Short catchy title in Myanmar and English"),
            "summary" to Schema.string(description = "Concise overview summary in natural Myanmar language"),
            "voiceScript" to Schema.string(description = "Complete Myanmar voiceover narration script suitable for speech synthesis"),
            "keyPoints" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 key bullet points in Myanmar"
            ),
            "srtSubtitles" to Schema.string(description = "Time-coded SRT subtitles in Myanmar")
        )
    )

    private val structuredRecapModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = recapSchema
            },
            systemInstruction = com.google.firebase.ai.type.content {
                text(
                    "You are an expert Myanmar video recap creator. You analyze video scripts, subtitles, transcripts or links, " +
                    "and produce structured JSON containing a catchy title, summary, spoken Myanmar narration script, key points, and SRT format subtitles."
                )
            }
        )
    }

    suspend fun generateRecap(
        content: String,
        sourceType: String,
        targetDurationMinutes: Int = 2
    ): Result<RecapResult> {
        return try {
            val prompt = """
                Please create a complete Myanmar Video Recap from the following $sourceType content.
                Requirements:
                1. Title: Catchy title in Myanmar.
                2. Summary: Clear, engaging Myanmar summary of what happened.
                3. VoiceScript: A fluid, natural spoken Myanmar narration script (အပြောစကားပြေ) designed to be read out loud at ~${targetDurationMinutes} minutes pace.
                4. KeyPoints: 3-5 crucial takeaway points in Myanmar.
                5. SrtSubtitles: Proper SRT format subtitles in Myanmar matching the flow (00:00:00,000 --> ...).

                Content:
                $content
            """.trimIndent()

            val response = withTimeout(TIMEOUT_MS) {
                structuredRecapModel.generateContent(prompt)
            }

            val jsonStr = response.text ?: throw IllegalStateException("Model returned empty text.")
            val json = JSONObject(jsonStr)

            val title = json.optString("title", "မြန်မာ Recap")
            val summary = json.optString("summary", "")
            val voiceScript = json.optString("voiceScript", summary)
            val keyPointsArray = json.optJSONArray("keyPoints") ?: JSONArray()
            val keyPoints = (0 until keyPointsArray.length()).map { keyPointsArray.getString(it) }
            val srtSubtitles = json.optString("srtSubtitles", "")

            Result.success(
                RecapResult(
                    title = title,
                    summary = summary,
                    voiceScript = voiceScript,
                    keyPoints = keyPoints,
                    srtSubtitles = srtSubtitles
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Recap generation timed out", e)
            Result.failure(Exception("Recap ပြုလုပ်ချိန် မိနစ်ပိုင်းကျော်လွန်သွားပါသည်။ ကျေးဇူးပြု၍ ပြန်လည်ကြိုးစားပါ။"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Recap generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun translateText(text: String, fastMode: Boolean = false): Result<String> {
        return try {
            val model = if (fastMode) liteModel else flashModel
            val prompt = """
                Translate the following text into natural, expressive, grammatically correct Myanmar (Burmese) language.
                Output ONLY the translated Myanmar text without explanations or headers.

                Text to translate:
                $text
            """.trimIndent()

            val response = withTimeout(TIMEOUT_MS) {
                model.generateContent(prompt)
            }
            val translated = response.text?.trim() ?: ""
            Result.success(translated)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("ဘာသာပြန်ခြင်း အချိန်ကျော်လွန်သွားပါသည်။"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Translation failed", e)
            Result.failure(e)
        }
    }

    suspend fun translateSrtSubtitles(srtContent: String): Result<String> {
        return try {
            val prompt = """
                Translate all subtitle dialogue in this SRT into natural Myanmar (Burmese) language.
                CRITICAL INSTRUCTION:
                - Keep the EXACT same index numbers and timestamps (e.g. 00:00:01,000 --> 00:00:04,000).
                - Only replace the text under each timestamp with the Myanmar translation.
                - Output valid raw SRT format only.

                SRT Content:
                $srtContent
            """.trimIndent()

            val response = withTimeout(TIMEOUT_MS) {
                flashModel.generateContent(prompt)
            }
            val output = response.text?.trim() ?: ""
            Result.success(output)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("SRT ဘာသာပြန်ခြင်း အချိန်ကျော်လွန်သွားပါသည်။"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "SRT translation failed", e)
            Result.failure(e)
        }
    }

    suspend fun recapSocialLink(url: String, platform: String): Result<RecapResult> {
        return try {
            val prompt = """
                The user wants a Myanmar Video Recap of this $platform link:
                URL: $url

                Please analyze this $platform video topic, context, title and content.
                Produce structured JSON with:
                - title: Catchy Myanmar title for this $platform video recap
                - summary: Engaging Myanmar recap summary
                - voiceScript: Fluid spoken Myanmar voiceover script for audio reading
                - keyPoints: 3-5 highlights in Myanmar
                - srtSubtitles: Synchronized Myanmar SRT subtitles
            """.trimIndent()

            val response = withTimeout(TIMEOUT_MS) {
                structuredRecapModel.generateContent(prompt)
            }

            val jsonStr = response.text ?: throw IllegalStateException("Model returned empty text.")
            val json = JSONObject(jsonStr)

            val title = json.optString("title", "$platform Myanmar Recap")
            val summary = json.optString("summary", "")
            val voiceScript = json.optString("voiceScript", summary)
            val keyPointsArray = json.optJSONArray("keyPoints") ?: JSONArray()
            val keyPoints = (0 until keyPointsArray.length()).map { keyPointsArray.getString(it) }
            val srtSubtitles = json.optString("srtSubtitles", "")

            Result.success(
                RecapResult(
                    title = title,
                    summary = summary,
                    voiceScript = voiceScript,
                    keyPoints = keyPoints,
                    srtSubtitles = srtSubtitles
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("လင့်ခ်မှ Recap ပြုလုပ်ခြင်း အချိန်ကျော်လွန်သွားပါသည်။"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Link recap failed", e)
            Result.failure(e)
        }
    }
}
