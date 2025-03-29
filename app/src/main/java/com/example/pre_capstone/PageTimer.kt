package com.example.pre_capstone

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pre_capstone.ActivityMain.Companion.timerStoppedReceiver
import com.google.firebase.auth.FirebaseAuth

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun DualStopwatchApp(
    timerName: String = "",
    workTime: Int = 60,
    restTime: Int = 10,
    category: Int = 1
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    var workTimer by remember { mutableStateOf(0) }
    var restTimer by remember { mutableStateOf(0) }
    var mulTime by remember { mutableStateOf(false) }
    var workCount by remember { mutableStateOf(1) }
    var activeStopwatch by remember { mutableStateOf(1) }
    var stopwatchRunning by remember { mutableStateOf(false) }
    var isStopped by remember { mutableStateOf(true) }
    var isChecked by remember { mutableStateOf(true) }
    var serviceConnection by remember { mutableStateOf<ServiceConnection?>(null) }
    var timerService by remember { mutableStateOf<TimerService?>(null) }

    DisposableEffect(Unit) {
        // 1. ServiceConnection 객체 정의
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as TimerService.TimerBinder
                timerService = binder.getService()

                // 콜백 설정
                timerService?.setCallback(object : TimerService.TimerCallback {
                    override fun onTimerTick(timerWorkTime: Int, timerRestTime: Int, timerActiveStopwatch: Int) {
                        workTimer = timerWorkTime
                        restTimer = timerRestTime
                        activeStopwatch = timerActiveStopwatch
                    }
                })
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                timerService = null
            }
        }

        // 2. BroadcastReceiver 객체Ti 별도 정의
        timerStoppedReceiver = TimerStoppedReceiver(){
            navController.navigate("main")
        }

        // 3. 리시버 등록
        Log.d("TAG", "onReceive: 리시버등록")
        context.registerReceiver(
            timerStoppedReceiver,
            IntentFilter("com.example.pre_capstone.TIMER_STOPPED"),
            Context.RECEIVER_EXPORTED
        )

        // 4. 서비스 연결 저장
        serviceConnection = connection

        // 5. 서비스 바인딩 시도
        val intent = Intent(context, TimerService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        // 6. 컴포저블이 제거될 때 정리 작업
        onDispose {
            try {
                context.unbindService(connection)
            } catch (e: Exception) {
                // 예외 처리
            }
        }
    }

    fun startStopwatch() {
        if (!stopwatchRunning && isStopped) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_START
                putExtra("timerName", timerName)
                putExtra("workTime", workTime)
                putExtra("restTime", restTime)
                putExtra("category", category)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)

            stopwatchRunning = true
            isStopped = false
        }
    }

    fun switchStopwatch() {
        if (stopwatchRunning) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_SWITCH
            }
            context.startService(intent)

            activeStopwatch = if (activeStopwatch == 1) 2 else 1
        }
    }

    fun stopStopwatch() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        context.startService(intent)

        isStopped = true
        stopwatchRunning = false

        navController.navigate("main")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var currentTimer = 0
        var maxTime = 0

        if (activeStopwatch == 1) {
            currentTimer = workTimer
            maxTime = workTime
        } else {
            currentTimer = restTimer
            maxTime = restTime
        }

        StopwatchUI(
            modifier = Modifier.size(300.dp),
            isChecked = isChecked,
            activeTimer = activeStopwatch,
            elapsedTime = currentTimer,
            maxTime = maxTime
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                if (!stopwatchRunning && isStopped) {
                    startStopwatch()
                } else if (stopwatchRunning) {
                    switchStopwatch()
                }
            }) {
                Text(if (!stopwatchRunning && isStopped) "시작" else "체인지")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(onClick = { stopStopwatch() }) {
                Text("종료")
            }
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = timerName, fontSize = 20.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "숫자 표시", fontSize = 20.sp)
                Switch(
                    modifier = Modifier,
                    checked = isChecked,
                    onCheckedChange = { isChecked = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color.Red
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "분단위", fontSize = 20.sp)
                Switch(
                    modifier = Modifier,
                    checked = mulTime,
                    onCheckedChange = {
                        mulTime = it
                        timerService?.setMinuteMultiplier(mulTime)
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color.Red
                    )
                )
            }
        }
    }
}

@Composable
fun StopwatchUI(
    elapsedTime: Int,
    activeTimer: Int,
    maxTime: Int,
    modifier: Modifier = Modifier,
    isChecked: Boolean
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        var step : Int = 0
        Canvas(modifier = modifier) {
            val strokeWidth = 50f
            val radius = size.minDimension / 2 - strokeWidth / 5

            // 배경 원 그리기 (전체 시간)
            drawCircle(
                color = Color.LightGray,
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // 진행 원 그리기 (경과 시간)
            var sweepAngle = if (maxTime > 0) {
                if(activeTimer ==1)
                    Math.min(360f,(360f/3 * (elapsedTime*3 / maxTime)))
                else
                    Math.min(360f,(360f * (elapsedTime.toFloat() / maxTime)))
            } else {
                0f
            }

            step = (sweepAngle/120).toInt()


            Log.i("color", "StopwatchUI: $step")
            drawArc(
                color = if (activeTimer == 1){
                    when(step){
                        1-> Color.Yellow
                        2-> Color.Magenta
                        3->Color.Red
                        else->Color.Red
                    }
                }
                else Color.Green,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
        }

        if (isChecked) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${elapsedTime / 60}:${(elapsedTime % 60).toString().padStart(2, '0')}",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeTimer == 1) Color.Black else Color.Gray,
                )

                Text(
                    text = "/ ${maxTime / 60}:${(maxTime % 60).toString().padStart(2, '0')}",
                    fontSize = 20.sp,
                    color = Color.Gray
                )
            }
        }
    }
}




class TimerStoppedReceiver(callback : () -> Unit = {}) : BroadcastReceiver() {
    val callBack = callback

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.pre_capstone.TIMER_STOPPED") {
            Log.d("TAG", "onReceive: 리시버받음")
            callBack()
        }
    }
}