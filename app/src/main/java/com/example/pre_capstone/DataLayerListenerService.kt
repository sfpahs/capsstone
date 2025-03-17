package com.example.pre_capstone

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class DataLayerListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "DataLayerService"
        private const val TIMER_STATUS_PATH = "/timer_status"
        private const val TIMER_ACTION_PATH = "/timer_action"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path

                if (path == TIMER_STATUS_PATH) {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val timerName = dataMap.getString("timer_name", "")
                    val activeStopwatch = dataMap.getInt("active_stopwatch", 1)
                    val workTimer = dataMap.getInt("work_timer", 0)
                    val restTimer = dataMap.getInt("rest_timer", 0)

                    // 워치 UI 업데이트를 위한 브로드캐스트 전송
                    val intent = Intent("com.example.pre_capstone.TIMER_UPDATE")
                    intent.putExtra("timer_name", timerName)
                    intent.putExtra("active_stopwatch", activeStopwatch)
                    intent.putExtra("work_timer", workTimer)
                    intent.putExtra("rest_timer", restTimer)
                    sendBroadcast(intent)

                    Log.d(TAG, "타이머 상태 수신: $timerName, 활성 타이머: $activeStopwatch")
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path

        when (path) {
            TIMER_ACTION_PATH -> {
                val action = String(messageEvent.data)

                // 타이머 서비스에 액션 전달
                val serviceIntent = Intent(this, TimerService::class.java)

                when (action) {
                    "start" -> serviceIntent.action = TimerService.ACTION_START
                    "pause" -> serviceIntent.action = TimerService.ACTION_PAUSE
                    "stop" -> serviceIntent.action = TimerService.ACTION_STOP
                    "switch" -> serviceIntent.action = TimerService.ACTION_SWITCH
                }

                startService(serviceIntent)
                Log.d(TAG, "타이머 액션 수신: $action")
            }

            "/navigation" -> {
                val destination = String(messageEvent.data)
                val intent = Intent("com.example.pre_capstone.NAVIGATION_ACTION")
                intent.putExtra("destination", destination)
                sendBroadcast(intent)
                Log.d(TAG, "네비게이션 요청 수신: $destination")
            }
        }
    }
}
