package com.example.pre_capstone

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.navigation.NavHostController
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ServiceWatchCommunication : Service(), MessageClient.OnMessageReceivedListener, DataClient.OnDataChangedListener {
    private val messageClient by lazy { Wearable.getMessageClient(this) }
    private val dataClient by lazy { Wearable.getDataClient(this) }
    private var timerService: TimerService? = null
    private var bound = false
    private val navController: NavHostController? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            bound = true

            // 타이머 상태 업데이트를 워치에 전송
            sendTimerStatusToWatch()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            bound = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        messageClient.addListener(this)
        dataClient.addListener(this)

        // TimerService에 바인딩
        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            "/timer_action" -> {
                var action = String(messageEvent.data)
                when (action) {
                    "switch" -> {
                        val intent = Intent(this, TimerService::class.java).apply {
                            action = TimerService.ACTION_SWITCH
                        }
                        startService(intent)
                    }
                    "stop" -> {
                        val intent = Intent(this, TimerService::class.java).apply {
                            action = TimerService.ACTION_STOP
                        }
                        startService(intent)
                    }
                }
            }
            "/navigation" -> {
                val destination = String(messageEvent.data)
                val broadcastIntent = Intent("com.example.pre_capstone.NAVIGATION_ACTION")
                broadcastIntent.putExtra("destination", destination)
                sendBroadcast(broadcastIntent)
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // 데이터 변경 처리 (필요한 경우)
    }

    private fun sendTimerStatusToWatch() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timerService = timerService ?: return@launch

                val request = PutDataMapRequest.create("/timer_status").apply {
                    dataMap.putString("timer_name", timerService.timerName)
                    dataMap.putInt("active_stopwatch", timerService.activeStopwatch)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }

                val putDataReq = request.asPutDataRequest()
                dataClient.putDataItem(putDataReq)
            } catch (e: Exception) {
                Log.e("WatchComm", "워치에 데이터 전송 실패: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        messageClient.removeListener(this)
        dataClient.removeListener(this)
        if (bound) {
            unbindService(connection)
            bound = false
        }
        super.onDestroy()
    }
}
