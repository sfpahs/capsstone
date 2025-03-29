package com.example.pre_capstone_watch.presentation

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
@Composable
fun WatchTimerControl(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(false) }
    var currentTimerName by remember { mutableStateOf("") }
    var activeStopwatch by remember { mutableStateOf(1) }

    // 데이터 레이어 클라이언트 초기화
    val messageClient = Wearable.getMessageClient(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (currentTimerName.isNotEmpty()) "$currentTimerName" else "연결 중...",
            fontSize = 16.sp,
            color = if (isConnected) Color.White else Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                sendMessageToPhone(messageClient, "/timer_action", "switch")
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("타이머 전환")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                sendMessageToPhone(messageClient, "/timer_action", "stop")
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("타이머 종료")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                sendMessageToPhone(messageClient, "/navigation", "main")
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("메인으로")
        }
    }

    // 데이터 수신 리스너 설정
    LaunchedEffect(Unit) {
        val dataClient = Wearable.getDataClient(context)

        dataClient.addListener { dataEvents ->
            dataEvents.forEach { event ->
                if (event.type == DataEvent.TYPE_CHANGED) {
                    val dataItem = event.dataItem
                    if (dataItem.uri.path == "/timer_status") {
                        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        isConnected = true
                        currentTimerName = dataMap.getString("timer_name", "")
                        activeStopwatch = dataMap.getInt("active_stopwatch", 1)
                    }
                }
            }
        }
    }
}

// 메시지 전송 함수
private fun sendMessageToPhone(messageClient: MessageClient, path: String, data: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val nodes = Wearable.getNodeClient(messageClient.applicationContext).connectedNodes.await()
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, path, data.toByteArray()).await()
            }
        } catch (e: Exception) {
            Log.e("WatchApp", "메시지 전송 실패: ${e.message}")
        }
    }
}

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        continuation.resume(result)
    }
    addOnFailureListener { exception ->
        continuation.resumeWithException(exception)
    }
}

