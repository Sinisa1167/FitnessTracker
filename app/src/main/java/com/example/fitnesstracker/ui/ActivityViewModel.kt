package com.example.fitnesstracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.Repository
import com.example.fitnesstracker.data.model.Activity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class ActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Repository(application)

    private val _searchQuery = MutableStateFlow("")
    private val _filterType = MutableStateFlow("")

    // Glavna lista aktivnosti sa filterom tipa
    val activities: StateFlow<List<Activity>> = _filterType
        .flatMapLatest { type ->
            if (type.isBlank()) repository.getAll()
            else repository.getByType(type)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Rezultati pretrage po tekstu
    val searchResults: StateFlow<List<Activity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getAll()
            else repository.search(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Ukupna distanca za danas - Čista reaktivna implementacija
    val todayDistance: StateFlow<Float> = repository
        .getTotalDistanceSince(startOfTodayMillis())
        .map { it ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Broj treninga za danas
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
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}