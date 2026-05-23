package com.example.fitnesstracker.data

import android.content.Context
import com.example.fitnesstracker.data.db.AppDatabase
import com.example.fitnesstracker.data.model.Activity
import kotlinx.coroutines.flow.Flow

class Repository(context: Context) {

    private val dao = AppDatabase.getInstance(context).activityDao()

    suspend fun insert(activity: Activity): Result<Long> = runCatching {
        dao.insert(activity)
    }

    suspend fun delete(activity: Activity): Result<Unit> = runCatching {
        dao.delete(activity)
    }

    suspend fun deleteByIds(ids: List<Long>): Result<Unit> = runCatching {
        dao.deleteByIds(ids)
    }

    suspend fun updateDescription(id: Long, description: String): Result<Unit> = runCatching {
        dao.updateDescription(id, description)
    }

    fun getAll(): Flow<List<Activity>> = dao.getAll()

    suspend fun getById(id: Long): Activity? = dao.getById(id)

    fun getCountSince(from: Long): Flow<Int> = dao.getCountSince(from)
}