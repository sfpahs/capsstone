package com.example.pre_capstone

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import com.example.pre_capstone.ui.theme.Pre_capstoneTheme

@SuppressLint("RestrictedApi")
class ActivityTimer : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val timerName = intent.getStringExtra("timerName") ?: ""
            val category = intent.getIntExtra("category", 1)
            val workTime = intent.getIntExtra("workTime", 60)
            val restTime = intent.getIntExtra("restTime", 10)
            Pre_capstoneTheme {
                DualStopwatchApp(
                    timerName = timerName,
                    category = category,
                    workTime = workTime,
                    restTime = restTime
                )
            }
        }
    }
}
