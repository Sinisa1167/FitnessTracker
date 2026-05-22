package com.example.fitnesstracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.fitnesstracker.data.model.ActivityType
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
    ActivityType.RUNNING.key  to ActivityGoal(5f, 30f),
    ActivityType.WALKING.key  to ActivityGoal(3f, 45f),
    ActivityType.CYCLING.key  to ActivityGoal(20f, 60f),
    ActivityType.SWIMMING.key to ActivityGoal(1f, 30f),
    ActivityType.HIKING.key   to ActivityGoal(8f, 120f),
    ActivityType.OTHER.key    to ActivityGoal(5f, 30f)
)

private const val FALLBACK_WEIGHT_KG = 75f

fun calculateCalories(
    activityType: String,
    durationSeconds: Long,
    profile: UserProfile,
    avgSpeedKmh: Float = 0f
): Int {
    val met = calculateMet(activityType, avgSpeedKmh)
    val durationHours = durationSeconds / 3600f
    val weight = if (profile.isConfigured) profile.weightKg else FALLBACK_WEIGHT_KG
    val base = met * weight * durationHours

    // korekcija za pol i godine ako su dostupni
    val corrected = if (profile.isConfigured && profile.ageYears > 0 && profile.isMale != null) {
        val ageFactor = 1f - ((profile.ageYears - 25).coerceAtLeast(0) * 0.003f)
        val sexFactor = if (profile.isMale) 1.0f else 0.9f
        base * ageFactor * sexFactor
    } else base

    return corrected.toInt()
}

fun calculateMet(type: String, speedKmh: Float): Float = when (ActivityType.fromKey(type)) {
    ActivityType.RUNNING -> when {
        speedKmh <= 0f  -> 9.8f
        speedKmh < 8f   -> 6.0f
        speedKmh < 10f  -> 8.3f
        speedKmh < 12f  -> 10.0f
        speedKmh < 14f  -> 11.5f
        speedKmh < 16f  -> 12.8f
        else            -> 14.5f
    }
    ActivityType.WALKING -> when {
        speedKmh <= 0f -> 3.5f
        speedKmh < 4f  -> 2.8f
        speedKmh < 5f  -> 3.5f
        speedKmh < 6f  -> 4.3f
        else           -> 5.0f
    }
    ActivityType.CYCLING -> when {
        speedKmh <= 0f  -> 7.5f
        speedKmh < 16f  -> 4.0f
        speedKmh < 20f  -> 6.8f
        speedKmh < 24f  -> 8.0f
        speedKmh < 30f  -> 10.0f
        else            -> 12.0f
    }
    ActivityType.SWIMMING -> 8.0f
    ActivityType.HIKING  -> 6.0f
    ActivityType.OTHER   -> 5.0f
    else            -> 5.0f
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

    suspend fun recordActivityCompleted() {
        context.dataStore.edit { it[KEY_LAST_ACTIVITY_TIME] = System.currentTimeMillis() }
        if (notificationsEnabled.first()) {
            ReminderWorker.scheduleFromNow(context)
        }
    }

    suspend fun getLastActivityTimestamp(): Long =
        context.dataStore.data.first()[KEY_LAST_ACTIVITY_TIME] ?: 0L
}