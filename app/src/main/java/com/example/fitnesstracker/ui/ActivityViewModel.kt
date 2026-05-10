package com.example.fitnesstracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.Repository
import com.example.fitnesstracker.data.model.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Repository(application)

    private val _searchQuery = MutableStateFlow("")
    private val _filterType = MutableStateFlow("")

    val activities: StateFlow<List<Activity>> = _filterType
        .flatMapLatest { type ->
            if (type.isBlank()) repository.getAll()
            else repository.getByType(type)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: StateFlow<List<Activity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getAll()
            else repository.search(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayDistance: StateFlow<Float> = repository
        .getTotalDistanceSince(startOfTodayMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
        .let { flow ->
            MutableStateFlow(0f).also { state ->
                viewModelScope.launch {
                    flow.collect { state.value = it ?: 0f }
                }
            }
        }

    val todayCount: StateFlow<Int> = repository
        .getCountSince(startOfTodayMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setFilter(type: String) {
        _filterType.value = type
    }

    fun setSearch(query: String) {
        _searchQuery.value = query
    }

    fun saveActivity(activity: Activity) {
        viewModelScope.launch {
            repository.insert(activity)
        }
    }

    fun deleteActivity(activity: Activity) {
        viewModelScope.launch {
            repository.delete(activity)
        }
    }

    suspend fun getById(id: Long): Activity? = repository.getById(id)

    private fun startOfTodayMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}