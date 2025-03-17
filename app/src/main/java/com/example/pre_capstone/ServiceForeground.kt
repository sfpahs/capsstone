package com.example.pre_capstone

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.pre_capstone.model.HistoryData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class TimerService : Service() {
    companion object {
        const val CHANNEL_ID = "TimerServiceChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.example.pre_capstone.START"
        const val ACTION_PAUSE = "com.example.pre_capstone.PAUSE"
        const val ACTION_STOP = "com.example.pre_capstone.STOP"
        const val ACTION_SWITCH = "com.example.pre_capstone.SWITCH"
    }

    // 콜백 인터페이스 정의
    interface TimerCallback {
        fun onTimerTick(workTimer: Int, restTimer: Int, activeStopwatch: Int)
    }

    private val binder = TimerBinder()
    private var timerJob: Job? = null
    private var isRunning = false
    var activeStopwatch = 1
    private var workTimer = 0
    private var restTimer = 0
    private var workTime = 0
    private var restTime = 0
    var timerName = ""
    private var category = 0
    private var totalWorkTime = 0
    private var totalRestTime = 0
    private var workCount = 1
    private var startTime = LocalDateTime.MIN
    private var mulTime = false
    private var callback: TimerCallback? = null

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        // 알림 권한 확인 (Android 13 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (!notificationManager.areNotificationsEnabled()) {
                val intent = Intent().apply {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("android.provider.extra.APP_PACKAGE", packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }

        // 정확한 알람 권한 확인 (Android 12 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = ContextCompat.getSystemService(this, android.app.AlarmManager::class.java)
            if (alarmManager?.canScheduleExactAlarms() == false) {
                val alarmIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(alarmIntent)
            }
        }

        // 삼성 기기 배터리 최적화 설정

    }
    //todo삼성배터리 최적화 찾아볼것
    fun samsungBattery(){
        if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            startActivity(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 서비스 시작 시 즉시 포그라운드로 전환
        startForeground(NOTIFICATION_ID, createNotification())

        when (intent?.action) {
            ACTION_START -> {
                timerName = intent.getStringExtra("timerName") ?: ""
                workTime = intent.getIntExtra("workTime", 60)
                restTime = intent.getIntExtra("restTime", 10)
                category = intent.getIntExtra("category", 1)
                startTimer()
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_STOP -> stopTimer()
            ACTION_SWITCH -> switchTimer()
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "타이머 알림 채널"
            channel.enableLights(false)
            channel.enableVibration(false)
            channel.setSound(null, null) // 소리 비활성화

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun setCallback(callback: TimerCallback) {
        this.callback = callback
    }

    fun setMinuteMultiplier(enabled: Boolean) {
        this.mulTime = enabled
    }

    private fun startTimer() {
        if (!isRunning) {
            isRunning = true
            startTime = LocalDateTime.now()
            startTimerJob()
        }
    }

    private fun pauseTimer() {
        isRunning = false
        timerJob?.cancel()
        updateNotification()
    }

    private fun stopTimer() {
        isRunning = false
        timerJob?.cancel()

        if (activeStopwatch == 1) totalWorkTime += workTimer
        else totalRestTime += restTimer

        totalWorkTime /= 60
        totalRestTime /= 60

        val historyData = HistoryData(
            startTime = startTime,
            category = category,
            totalMinute = totalWorkTime + totalRestTime,
            workingMinute = totalWorkTime,
            restMinute = totalRestTime,
            averageWorkingMinute = totalWorkTime / workCount
        )

        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            saveHistoryData(historyData, it.uid)
        }

        Log.d("TAG", "onReceive: 리시버던짐")
        // 브로드캐스트 전송 - UI에 타이머 종료 알림
        val broadcastIntent = Intent("com.example.pre_capstone.TIMER_STOPPED")
        broadcastIntent.putExtra("navigate_to_main", true)
        broadcastIntent.flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        sendBroadcast(broadcastIntent)

        // 알림 제거 및 서비스 중지
        removeNotificationAndStopService()
    }

    private fun switchTimer() {
        timerJob?.cancel()

        activeStopwatch = if (activeStopwatch == 1) {
            totalWorkTime += workTimer
            workTimer = 0
            2
        } else {
            totalRestTime += restTimer
            workCount++
            restTimer = 0
            1
        }

        startTimerJob()
        updateNotification()
    }

    private fun startTimerJob() {
        timerJob = CoroutineScope(Dispatchers.Default).launch {
            while (isRunning) {
                delay(1000L)
                if (activeStopwatch == 1) {
                    workTimer += 1 * if (mulTime) 60 else 1
                } else {
                    restTimer += 1 * if (mulTime) 60 else 1
                }
                // 콜백으로 UI 업데이트
                callback?.onTimerTick(workTimer, restTimer, activeStopwatch)
                updateNotification()
            }
        }
    }

    private fun createNotification(): Notification {
        // 알림 바디 클릭 시 앱 실행 인텐트
        val notificationIntent = Intent(this, ActivityMain::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 스위치 버튼 인텐트
        val switchIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_SWITCH
        }
        val switchPendingIntent = PendingIntent.getService(
            this, 1, switchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 종료 버튼 인텐트 - 앱을 실행하지 않고 서비스만 종료하는 특별 인텐트
        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val currentTimer = if (activeStopwatch == 1) workTimer else restTimer
        val maxTime = if (activeStopwatch == 1) workTime else restTime
        val timerText = "${currentTimer / 60}:${(currentTimer % 60).toString().padStart(2, '0')} / ${maxTime / 60}:${(maxTime % 60).toString().padStart(2, '0')}"
        val activityType = if (activeStopwatch == 1) "작업 중" else "휴식 중"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$timerName - $activityType")
            .setContentText(timerText)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_switch, "체인지", switchPendingIntent)
            .addAction(R.drawable.ic_stop, "종료", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("NotificationPermission")
    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun removeNotificationAndStopService() {
        // 알림 매니저를 통해 직접 알림 제거
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()

        // 안드로이드 버전에 따라 다른 방식으로 포그라운드 서비스 중지
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }

        // 서비스 자체를 중지
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        removeNotificationAndStopService()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        timerJob?.cancel()
        super.onDestroy()
    }
}
