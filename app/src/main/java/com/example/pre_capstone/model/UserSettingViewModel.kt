package com.example.pre_capstone.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pre_capstone.loadTimerSettingsFireBase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {
    private val _timerSettings = MutableStateFlow<List<TimerSetting>>(emptyList())
    val timerSettings: StateFlow<List<TimerSetting>> = _timerSettings

    private val database = FirebaseDatabase.getInstance().reference

    // 타이머 설정 불러오기
    fun loadTimerSettings(userId: String) {
        viewModelScope.launch {
            loadTimerSettingsFireBase(uid = userId,
                onSuccess = { settings ->
                    _timerSettings.value = settings
                    Log.i("firebase", "loadTimerSettings: ${settings}")
                },
                onFailure = { e ->
                    Log.e("firebaseError", "loadTimerSettings: ${e.message}",)
                })

        }
    }

    // 타이머 설정 저장
    fun saveTimerSettings(userId: String, settings: List<TimerSetting>) {
        viewModelScope.launch {
            val updates = settings.associateBy { it.index.toString() }
            database.child("users").child(userId).child("settings").child("timer")
                .setValue(updates)
                .addOnSuccessListener {
                    _timerSettings.value = settings // 로컬 상태 업데이트
                }
                .addOnFailureListener { exception ->
                    Log.e("viewModelRoutin", "saveTimerSettings: ${exception.message}",)
                    // 실패 처리 (로그 출력 등)
                }
        }
    }
}