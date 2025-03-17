package com.example.pre_capstone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.pre_capstone.ui.theme.Pre_capstoneTheme

class ActivityMain : ComponentActivity() {
    private lateinit var navController: NavHostController
    private lateinit var navigationReceiver: BroadcastReceiver
    companion object{
        lateinit var timerStoppedReceiver : TimerStoppedReceiver
    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, ServiceWatchCommunication::class.java))
        // 네비게이션 리시버 등록
        navigationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "com.example.pre_capstone.NAVIGATION_ACTION") {
                    val destination = intent.getStringExtra("destination") ?: return
                    runOnUiThread {
                        when (destination) {
                            "main" -> navController.navigate("main")
                            // 다른 화면으로의 이동도 추가 가능
                        }
                    }
                }
            }
        }

        registerReceiver(navigationReceiver, IntentFilter("com.example.pre_capstone.NAVIGATION_ACTION"),
            RECEIVER_NOT_EXPORTED
        )
        enableEdgeToEdge()
        setContent {
            Pre_capstoneTheme {
                navController = rememberNavController()
                MyBottomNavi(navController = navController)
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(navigationReceiver)
            unregisterReceiver(timerStoppedReceiver)
        }catch (e:Exception){}
    }

}

