package com.example.fitnesstracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.ActivityGoal
import com.example.fitnesstracker.data.DEFAULT_GOALS
import com.example.fitnesstracker.data.PreferencesManager
import com.example.fitnesstracker.data.Repository
import com.example.fitnesstracker.data.UserProfile
import com.example.fitnesstracker.data.model.Activity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

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

    private val _searchQuery = MutableStateFlow("")
    private val _filterType  = MutableStateFlow("")

    val units: StateFlow<String> = prefs.units
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "km")

    val userProfile: StateFlow<UserProfile> = prefs.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val activities: StateFlow<List<Activity>> = _filterType
        .flatMapLatest { type ->
            if (type.isBlank()) repository.getAll() else repository.getByType(type)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: StateFlow<List<Activity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getAll() else repository.search(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayDistance: StateFlow<Float> = repository
        .getTotalDistanceSince(startOfTodayMillis())
        .map { it ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val todayCount: StateFlow<Int> = repository
        .getCountSince(startOfTodayMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayStatsByType: StateFlow<List<TypeDayStat>> = combine(
        repository.getAll(),
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

    fun setFilter(type: String) { _filterType.value = type }
    fun setSearch(query: String) { _searchQuery.value = query }

    fun saveActivity(activity: Activity) {
        viewModelScope.launch {
            repository.insert(activity)
            prefs.recordActivityCompleted(getApplication())
        }
    }

    fun deleteActivity(activity: Activity) {
        viewModelScope.launch { repository.delete(activity) }
    }

    suspend fun getById(id: Long): Activity? = repository.getById(id)

    private fun startOfTodayMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun defaultTodayStats() = DEFAULT_GOALS.keys.map { type ->
        TypeDayStat(type, 0f, 0L, 0f, DEFAULT_GOALS[type]!!)
    }
}