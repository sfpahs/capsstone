package com.example.pre_capstone.model

data class TimerRecord(
    val day: String, // 요일 (월, 화, 수 등)
    val startTime: String, // 타이머 시작 시간
    val durationMinutes: Int // 측정된 시간 (분 단위)
)
