package com.example.fitnesstracker.data.model

enum class ActivityType(val key: String) {
    RUNNING("Trčanje"),
    WALKING("Hodanje"),
    CYCLING("Biciklizam"),
    SWIMMING("Plivanje"),
    HIKING("Planinarenje"),
    OTHER("Ostalo");

    companion object {
        fun fromKey(key: String) = ActivityType.entries.firstOrNull { it.key == key } ?: OTHER
        val allKeys get() = ActivityType.entries.map { it.key }
    }
}