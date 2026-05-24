package com.example.fitnesstracker.data.model

enum class ActivityType(val key: String) {
    RUNNING("running"),
    WALKING("walking"),
    CYCLING("cycling"),
    SWIMMING("swimming"),
    HIKING("hiking"),
    OTHER("other");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: OTHER
        val allKeys = entries.map { it.key }
    }
}