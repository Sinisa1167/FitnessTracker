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

data class ActivityGoal(val distanceKm: Float, val durationMin: Float)

val DEFAULT_GOALS = mapOf(
    "Trčanje"    to ActivityGoal(5f, 30f),
    "Hodanje"    to ActivityGoal(3f, 45f),
    "Biciklizam" to ActivityGoal(20f, 60f),
    "Plivanje"   to ActivityGoal(1f, 30f),
    "Ostalo"     to ActivityGoal(5f, 30f)
)

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_LANGUAGE      = stringPreferencesKey("language")
        val KEY_UNITS         = stringPreferencesKey("units")
        val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications")

        fun goalDistanceKey(type: String) = floatPreferencesKey("goal_distance_$type")
        fun goalDurationKey(type: String) = floatPreferencesKey("goal_duration_$type")
    }

    val language: Flow<String> = context.dataStore.data.map { it[KEY_LANGUAGE] ?: "sr" }
    val units: Flow<String>    = context.dataStore.data.map { it[KEY_UNITS] ?: "km" }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFICATIONS] ?: true }

    fun goalForType(type: String): Flow<ActivityGoal> = context.dataStore.data.map { prefs ->
        val default = DEFAULT_GOALS[type] ?: ActivityGoal(5f, 30f)
        ActivityGoal(
            distanceKm  = prefs[goalDistanceKey(type)] ?: default.distanceKm,
            durationMin = prefs[goalDurationKey(type)] ?: default.durationMin
        )
    }

    val allGoals: Flow<Map<String, ActivityGoal>> = context.dataStore.data.map { prefs ->
        DEFAULT_GOALS.keys.associateWith { type ->
            val default = DEFAULT_GOALS[type]!!
            ActivityGoal(
                distanceKm  = prefs[goalDistanceKey(type)] ?: default.distanceKm,
                durationMin = prefs[goalDurationKey(type)] ?: default.durationMin
            )
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    suspend fun setUnits(u: String) {
        context.dataStore.edit { it[KEY_UNITS] = u }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
        if (enabled) com.example.fitnesstracker.worker.ReminderWorker.schedule(context)
        else         com.example.fitnesstracker.worker.ReminderWorker.cancel(context)
    }

    suspend fun setGoalDistance(type: String, value: Float) {
        context.dataStore.edit { it[goalDistanceKey(type)] = value }
    }

    suspend fun setGoalDuration(type: String, value: Float) {
        context.dataStore.edit { it[goalDurationKey(type)] = value }
    }
}