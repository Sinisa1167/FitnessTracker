package com.example.fitnesstracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.fitnesstracker.worker.ReminderWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class ActivityGoal(val distanceKm: Float, val durationMin: Float)

// weightKg == 0f means "not configured" — calorie calc falls back to average
data class UserProfile(
    val weightKg: Float  = 0f,
    val heightCm: Float  = 0f,
    val ageYears: Int    = 0,
    val isMale: Boolean? = null
) {
    val isConfigured: Boolean get() = weightKg > 0f
}

val DEFAULT_GOALS = mapOf(
    "Trčanje"    to ActivityGoal(5f, 30f),
    "Hodanje"    to ActivityGoal(3f, 45f),
    "Biciklizam" to ActivityGoal(20f, 60f),
    "Plivanje"   to ActivityGoal(1f, 30f),
    "Ostalo"     to ActivityGoal(5f, 30f)
)

val MET_VALUES = mapOf(
    "Trčanje"    to 9.8f,
    "Hodanje"    to 3.5f,
    "Biciklizam" to 7.5f,
    "Plivanje"   to 8.0f,
    "Ostalo"     to 5.0f
)

private const val FALLBACK_WEIGHT_KG = 75f

fun calculateCalories(activityType: String, durationSeconds: Long, profile: UserProfile): Int {
    val met           = MET_VALUES[activityType] ?: 5.0f
    val durationHours = durationSeconds / 3600f
    val weight        = if (profile.isConfigured) profile.weightKg else FALLBACK_WEIGHT_KG
    return (met * weight * durationHours).toInt()
}

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_LANGUAGE           = stringPreferencesKey("language")
        val KEY_UNITS              = stringPreferencesKey("units")
        val KEY_NOTIFICATIONS      = booleanPreferencesKey("notifications")
        val KEY_REMINDER_HOURS = intPreferencesKey("reminder_hours")

        val KEY_LAST_ACTIVITY_TIME = longPreferencesKey("last_activity_timestamp")
        val KEY_WEIGHT_KG          = floatPreferencesKey("weight_kg")
        val KEY_HEIGHT_CM          = floatPreferencesKey("height_cm")
        val KEY_AGE_YEARS          = intPreferencesKey("age_years")
        val KEY_IS_MALE            = booleanPreferencesKey("is_male")
        val KEY_IS_MALE_SET        = booleanPreferencesKey("is_male_set")

        fun goalDistanceKey(type: String) = floatPreferencesKey("goal_distance_$type")
        fun goalDurationKey(type: String) = floatPreferencesKey("goal_duration_$type")
    }

    val language: Flow<String> = context.dataStore.data.map { it[KEY_LANGUAGE] ?: "sr" }
    val units: Flow<String>    = context.dataStore.data.map { it[KEY_UNITS] ?: "km" }
    val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_NOTIFICATIONS] ?: true }
    val lastActivityTimestamp: Flow<Long> =
        context.dataStore.data.map { it[KEY_LAST_ACTIVITY_TIME] ?: 0L }

    val reminderHours: Flow<Int> = context.dataStore.data.map { it[KEY_REMINDER_HOURS] ?: 48 }

    suspend fun setReminderHours(hours: Int) {
        context.dataStore.edit { it[KEY_REMINDER_HOURS] = hours }
    }

    val userProfile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            weightKg = prefs[KEY_WEIGHT_KG] ?: 0f,
            heightCm = prefs[KEY_HEIGHT_CM] ?: 0f,
            ageYears = prefs[KEY_AGE_YEARS] ?: 0,
            isMale   = if (prefs[KEY_IS_MALE_SET] == true) prefs[KEY_IS_MALE] else null
        )
    }

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
        if (enabled) ReminderWorker.scheduleFromNow(context)
        else         ReminderWorker.cancel(context)
    }

    suspend fun setGoalDistance(type: String, value: Float) {
        context.dataStore.edit { it[goalDistanceKey(type)] = value }
    }

    suspend fun setGoalDuration(type: String, value: Float) {
        context.dataStore.edit { it[goalDurationKey(type)] = value }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WEIGHT_KG] = profile.weightKg
            prefs[KEY_HEIGHT_CM] = profile.heightCm
            prefs[KEY_AGE_YEARS] = profile.ageYears
            if (profile.isMale != null) {
                prefs[KEY_IS_MALE]     = profile.isMale
                prefs[KEY_IS_MALE_SET] = true
            } else {
                prefs.remove(KEY_IS_MALE)
                prefs.remove(KEY_IS_MALE_SET)
            }
        }
    }

    suspend fun recordActivityCompleted(context: Context) {
        context.dataStore.edit { it[KEY_LAST_ACTIVITY_TIME] = System.currentTimeMillis() }
        if (notificationsEnabled.first()) {
            ReminderWorker.scheduleFromNow(context)
        }
    }

    suspend fun getLastActivityTimestamp(): Long =
        context.dataStore.data.first()[KEY_LAST_ACTIVITY_TIME] ?: 0L
}