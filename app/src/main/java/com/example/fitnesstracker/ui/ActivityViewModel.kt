package com.example.fitnesstracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.ActivityGoal
import com.example.fitnesstracker.data.DEFAULT_GOALS
import com.example.fitnesstracker.data.PreferencesManager
import com.example.fitnesstracker.data.Repository
import com.example.fitnesstracker.data.UserProfile
import com.example.fitnesstracker.data.model.Activity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.fitnesstracker.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TypeDayStat(
    val type: String,
    val distanceMeters: Float,
    val durationSeconds: Long,
    val avgSpeedKmh: Float,
    val goal: ActivityGoal
)

class ActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Repository(application)
    private val prefs      = PreferencesManager(application)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val units: StateFlow<String> = prefs.units
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "km")

    val userProfile: StateFlow<UserProfile> = prefs.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val activities: StateFlow<List<Activity>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoals: StateFlow<Map<String, ActivityGoal>> = prefs.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val todayCount: StateFlow<Int> = repository
        .getCountSince(startOfTodayMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayStatsByType: StateFlow<List<TypeDayStat>> = combine(
        activities,
        prefs.allGoals
    ) { all, goals ->
        val todayStart      = startOfTodayMillis()
        val todayActivities = all.filter { it.timestamp >= todayStart }
        DEFAULT_GOALS.keys.map { type ->
            val typeActivities = todayActivities.filter { it.type == type }
            TypeDayStat(
                type            = type,
                distanceMeters  = typeActivities.sumOf { it.distanceMeters.toDouble() }.toFloat(),
                durationSeconds = typeActivities.sumOf { it.durationSeconds },
                avgSpeedKmh     = typeActivities.map { it.avgSpeedKmh }.filter { it > 0f }
                    .average().toFloat().takeIf { it.isFinite() } ?: 0f,
                goal            = goals[type] ?: DEFAULT_GOALS[type]!!
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultTodayStats())

    fun saveActivity(activity: Activity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insert(activity)
                .onSuccess {
                    prefs.recordActivityCompleted()
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                }
                .onFailure {
                    _error.value = getApplication<Application>()
                        .getString(R.string.error_save_activity)
                }
        }
    }

    fun deleteActivity(activity: Activity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.delete(activity)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                }
                .onFailure {
                    _error.value = getApplication<Application>()
                        .getString(R.string.error_delete_activity)
                }
        }
    }

    fun deleteActivities(ids: Set<Long>) {
        viewModelScope.launch {
            repository.deleteByIds(ids.toList())
                .onFailure {
                    _error.value = getApplication<Application>()
                        .getString(R.string.error_delete_activity)
                }
        }
    }

    fun updateDescription(id: Long, description: String) {
        viewModelScope.launch {
            repository.updateDescription(id, description)
                .onFailure {
                    _error.value = getApplication<Application>()
                        .getString(R.string.error_update_description)
                }
        }
    }

    fun clearError() { _error.value = null }

    suspend fun getById(id: Long): Activity? = repository.getById(id)

    private fun startOfTodayMillis() = DateUtils.startOfTodayMillis()

    private fun defaultTodayStats() = DEFAULT_GOALS.keys.map { type ->
        TypeDayStat(type, 0f, 0L, 0f, DEFAULT_GOALS[type]!!)
    }

    fun setGoalDistance(type: String, value: Float) {
        viewModelScope.launch { prefs.setGoalDistance(type, value) }
    }

    fun setGoalDuration(type: String, value: Float) {
        viewModelScope.launch { prefs.setGoalDuration(type, value) }
    }
}