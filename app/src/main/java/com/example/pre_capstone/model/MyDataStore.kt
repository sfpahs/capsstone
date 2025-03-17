package com.example.pre_capstone.model

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore


class DataStoreManager(private val context: Context) {
    private val Context.dataStore by preferencesDataStore(name = "settings")

    private val themeKey = stringPreferencesKey("theme")
    private val counterKey = intPreferencesKey("counter")

    // DataStore 인스턴스 가져오기
    val dataStore = context.dataStore







}
