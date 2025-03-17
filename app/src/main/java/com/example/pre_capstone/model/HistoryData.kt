package com.example.pre_capstone.model

import java.time.LocalDateTime

data class HistoryData(
    var startTime : LocalDateTime,
    var category : Int,
    var totalMinute : Int,
    var workingMinute : Int,
    var restMinute : Int,
    var averageWorkingMinute : Int,
)
