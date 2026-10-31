package com.example.util

import com.example.data.model.SrtCue
import java.util.Locale
import java.util.regex.Pattern

object SrtParser {

    private val TIME_PATTERN = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})[,.](\\d{3})")

    fun parse(srtContent: String): List<SrtCue> {
        val cues = mutableListOf<SrtCue>()
        if (srtContent.isBlank()) return cues

        val normalized = srtContent.replace("\r\n", "\n").replace("\r", "\n")
        val blocks = normalized.split("\n\n")

        var autoIndex = 1
        for (block in blocks) {
            val lines = block.lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) continue

            var timeLineIdx = -1
            for (i in lines.indices) {
                if (lines[i].contains("-->")) {
                    timeLineIdx = i
                    break
                }
            }

            if (timeLineIdx == -1) continue

            val timeParts = lines[timeLineIdx].split("-->")
            if (timeParts.size < 2) continue

            val startStr = timeParts[0].trim()
            val endStr = timeParts[1].trim()

            val startMs = parseTimeToMs(startStr)
            val endMs = parseTimeToMs(endStr)

            val textLines = lines.subList(timeLineIdx + 1, lines.size)
            val text = textLines.joinToString("\n").trim()

            cues.add(
                SrtCue(
                    index = autoIndex++,
                    startTimeMs = startMs,
                    endTimeMs = endMs,
                    startFormatted = startStr,
                    endFormatted = endStr,
                    originalText = text,
                    translatedMyanmarText = ""
                )
            )
        }

        return cues
    }

    fun parseTimeToMs(timeStr: String): Long {
        val matcher = TIME_PATTERN.matcher(timeStr)
        if (!matcher.find()) return 0L
        val hours = matcher.group(1)?.toLongOrNull() ?: 0L
        val minutes = matcher.group(2)?.toLongOrNull() ?: 0L
        val seconds = matcher.group(3)?.toLongOrNull() ?: 0L
        val millis = matcher.group(4)?.toLongOrNull() ?: 0L
        return (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
    }

    fun formatMsToSrtTime(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }

    fun exportToSrt(cues: List<SrtCue>, useTranslated: Boolean = true): String {
        val sb = StringBuilder()
        cues.forEachIndexed { idx, cue ->
            sb.append(idx + 1).append("\n")
            sb.append(formatMsToSrtTime(cue.startTimeMs))
                .append(" --> ")
                .append(formatMsToSrtTime(cue.endTimeMs))
                .append("\n")
            val text = if (useTranslated && cue.translatedMyanmarText.isNotBlank()) {
                cue.translatedMyanmarText
            } else {
                cue.originalText
            }
            sb.append(text).append("\n\n")
        }
        return sb.toString().trimEnd()
    }
}
