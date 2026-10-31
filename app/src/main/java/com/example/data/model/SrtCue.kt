package com.example.data.model

data class SrtCue(
    val index: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val startFormatted: String,
    val endFormatted: String,
    val originalText: String,
    val translatedMyanmarText: String = ""
)
