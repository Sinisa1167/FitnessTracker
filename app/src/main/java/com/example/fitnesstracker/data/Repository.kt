package com.example.fitnesstracker.data

import android.content.Context
import com.example.fitnesstracker.data.db.AppDatabase
import com.example.fitnesstracker.data.model.Activity
import kotlinx.coroutines.flow.Flow

class Repository(context: Context) {

    private val dao = AppDatabase.getInstance(context).activityDao()

    suspend fun insert(activity: Activity) = dao.insert(activity)

    suspend fun delete(activity: Activity) = dao.delete(activity)

    fun getAll(): Flow<List<Activity>> = dao.getAll()

    suspend fun getById(id: Long): Activity? = dao.getById(id)

    fun getByType(type: String): Flow<List<Activity>> = dao.getByType(type)

    fun getByDateRange(from: Long, to: Long): Flow<List<Activity>> = dao.getByDateRange(from, to)

    fun search(query: String): Flow<List<Activity>> = dao.search("%$query%")

    fun getTotalDistanceSince(from: Long): Flow<Float?> = dao.getTotalDistanceSince(from)

    fun getCountSince(from: Long): Flow<Int> = dao.getCountSince(from)
}