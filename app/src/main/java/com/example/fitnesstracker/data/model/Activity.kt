package com.example.fitnesstracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class Activity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val durationSeconds: Long,
    val distanceMeters: Float,
    val timestamp: Long,
    val description: String = "",
    val avgSpeedKmh: Float = 0f,
    val gpsPoints: String = ""
)