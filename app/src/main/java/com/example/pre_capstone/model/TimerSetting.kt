package com.example.pre_capstone.model

import androidx.compose.ui.graphics.Color

data class TimerSetting(
    val index: Int = 0,
    val name: String = "",
    val backgroundColor: Long = 0xFFFFFFFF,
    val workTime: Int = 0,
    val restTime: Int = 0
)
