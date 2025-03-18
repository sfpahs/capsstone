package com.example.pre_capstone_watch
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WatchDataLayerListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WatchDataLayerService"
        private const val TIMER_STATUS_PATH = "/timer_status"
        private const val TIMER_ACTION_PATH = "/timer_action"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged: 데이터 받은")
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
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

                    Log.d(TAG, "워치: 타이머 상태 수신: $timerName, 활성 타이머: $activeStopwatch")
                    Log.d(TAG, "워치: 타이머 맵: $dataMap")
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path

        when (path) {
            TIMER_ACTION_PATH -> {
                val action = String(messageEvent.data)

                // 워치에서는 일반적으로 UI 업데이트를 위한 브로드캐스트 전송
                val intent = Intent("com.example.pre_capstone.TIMER_ACTION")
                intent.putExtra("action", action)
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

                Log.d(TAG, "워치: 타이머 액션 수신: $action")
            }
        }
    }
}