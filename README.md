# Fitness Tracker — Android App

An Android application for tracking physical activities in real time. Built with Kotlin following Material Design guidelines, with a focus on clean UI and smooth performance.

## Features

- **Real-time Tracking** - GPS-based distance and route tracking during activities; displays live speed, average speed, duration and distance
- **Activity History** - Local SQLite database; browse, search and filter by type, date or duration
- **Route Map** - View GPS route on a map for each recorded activity
- **Statistics Charts** - Bar and line charts showing distance over time, activity count by type
- **Goals & Progress** - Set daily goals (distance, duration) and track progress
- **Notifications** - Reminders if you haven't been active within a configured time window
- **Settings** - Language (SR/EN), units (km/miles), notification toggle
- **Multi-screen Support** - Vector assets, responsive layouts tested on phones and tablets at various resolutions

## Tech Stack

- **Language** - Kotlin
- **Architecture** - Async operations via coroutines
- **Database** - SQLite (local storage)
- **Location** - Android GPS / Location Services
- **UI** - Material Design 3, ConstraintLayout, vector drawables

## Getting Started

1. Clone the repo and open in Android Studio
2. Build and run on an emulator or physical device (Android 8.0+)

```bash
git clone https://github.com/Sinisa1167/FitnessTracker.git
```

Requires location permissions for GPS tracking.
