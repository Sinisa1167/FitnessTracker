package com.example.fitnesstracker.data.db

import androidx.room.*
import com.example.fitnesstracker.data.model.Activity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Insert
    suspend fun insert(activity: Activity): Long

    @Delete
    suspend fun delete(activity: Activity)

    @Query("UPDATE activities SET description = :description WHERE id = :id")
    suspend fun updateDescription(id: Long, description: String)

    @Query("SELECT * FROM activities ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Activity>>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getById(id: Long): Activity?

    @Query("SELECT * FROM activities WHERE type = :type ORDER BY timestamp DESC")
    fun getByType(type: String): Flow<List<Activity>>

    @Query("SELECT * FROM activities WHERE timestamp >= :from AND timestamp <= :to ORDER BY timestamp DESC")
    fun getByDateRange(from: Long, to: Long): Flow<List<Activity>>

    @Query("SELECT * FROM activities WHERE type LIKE :query OR description LIKE :query OR strftime('%d.%m.%Y', datetime(timestamp/1000, 'unixepoch')) LIKE :query ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<Activity>>

    @Query("SELECT SUM(distanceMeters) FROM activities WHERE timestamp >= :from")
    fun getTotalDistanceSince(from: Long): Flow<Float?>

    @Query("SELECT COUNT(*) FROM activities WHERE timestamp >= :from")
    fun getCountSince(from: Long): Flow<Int>
}