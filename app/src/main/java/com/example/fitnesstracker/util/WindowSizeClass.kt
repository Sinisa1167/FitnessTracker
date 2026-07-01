package com.example.fitnesstracker.util

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthSizeClass { COMPACT, MEDIUM, EXPANDED }

val LocalWindowWidthSizeClass = compositionLocalOf { WindowWidthSizeClass.COMPACT }

fun windowWidthSizeClassOf(widthDp: Int): WindowWidthSizeClass = when {
    widthDp < 600 -> WindowWidthSizeClass.COMPACT
    widthDp < 840 -> WindowWidthSizeClass.MEDIUM
    else -> WindowWidthSizeClass.EXPANDED
}

@Composable
fun ProvideWindowSizeClass(content: @Composable () -> Unit) {
    val widthClass = windowWidthSizeClassOf(LocalConfiguration.current.screenWidthDp)
    CompositionLocalProvider(LocalWindowWidthSizeClass provides widthClass) {
        content()
    }
}

fun Modifier.responsiveMaxWidth(maxWidth: Dp = 640.dp): Modifier = composed {
    val widthClass = LocalWindowWidthSizeClass.current
    if (widthClass == WindowWidthSizeClass.COMPACT) this else this.widthIn(max = maxWidth)
}