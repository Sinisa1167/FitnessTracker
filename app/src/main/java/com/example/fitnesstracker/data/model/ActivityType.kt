package com.example.fitnesstracker.data.model

enum class ActivityType(val key: String) {
    RUNNING("Trčanje"),
    WALKING("Hodanje"),
    CYCLING("Biciklizam"),
    SWIMMING("Plivanje"),
    HIKING("Planinarenje"),
    OTHER("Ostalo");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: OTHER
        val allKeys = entries.map { it.key }
    }
}