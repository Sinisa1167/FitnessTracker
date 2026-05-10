package com.example.fitnesstracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_UNITS = stringPreferencesKey("units")
        val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications")
        val KEY_GOAL_DISTANCE = floatPreferencesKey("goal_distance")
        val KEY_GOAL_DURATION = floatPreferencesKey("goal_duration")
    }

    val language: Flow<String> = context.dataStore.data
        .map { it[KEY_LANGUAGE] ?: "sr" }

    val units: Flow<String> = context.dataStore.data
        .map { it[KEY_UNITS] ?: "km" }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_NOTIFICATIONS] ?: true }

    val goalDistance: Flow<Float> = context.dataStore.data
        .map { it[KEY_GOAL_DISTANCE] ?: 5f }

    val goalDuration: Flow<Float> = context.dataStore.data
        .map { it[KEY_GOAL_DURATION] ?: 30f }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    suspend fun setUnits(units: String) {
        context.dataStore.edit { it[KEY_UNITS] = units }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
        if (enabled) {
            com.example.fitnesstracker.worker.ReminderWorker.schedule(context)
        } else {
            com.example.fitnesstracker.worker.ReminderWorker.cancel(context)
        }
    }

    suspend fun setGoalDistance(value: Float) {
        context.dataStore.edit { it[KEY_GOAL_DISTANCE] = value }
    }

    suspend fun setGoalDuration(value: Float) {
        context.dataStore.edit { it[KEY_GOAL_DURATION] = value }
    }
}