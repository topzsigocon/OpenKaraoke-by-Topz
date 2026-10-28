package com.topz.karaoke.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Dark blue + gold. */
object KColors {
    val Navy = Color(0xFF0A1633)
    val NavySurface = Color(0xFF11224D)
    val NavyLight = Color(0xFF1C336B)
    val Gold = Color(0xFFF2C14E)
    val GoldDim = Color(0xFF9C7B2E)
    val Cream = Color(0xFFF7EFD6)
    val CreamDim = Color(0xFFB9B39F)
}

private val KaraokeColorScheme = darkColorScheme(
    primary = KColors.Gold,
    onPrimary = KColors.Navy,
    secondary = KColors.Gold,
    onSecondary = KColors.Navy,
    background = KColors.Navy,
    onBackground = KColors.Cream,
    surface = KColors.NavySurface,
    onSurface = KColors.Cream,
    surfaceVariant = KColors.NavyLight,
    onSurfaceVariant = KColors.CreamDim,
    secondaryContainer = KColors.NavyLight,
    onSecondaryContainer = KColors.Gold,
    primaryContainer = KColors.NavyLight,
    onPrimaryContainer = KColors.Gold,
    outline = KColors.GoldDim
)

@Composable
fun KaraokeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KaraokeColorScheme, content = content)
}

@Composable
fun fieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = KColors.Gold,
    unfocusedBorderColor = KColors.GoldDim,
    cursorColor = KColors.Gold,
    focusedTextColor = KColors.Cream,
    unfocusedTextColor = KColors.Cream,
    focusedPlaceholderColor = KColors.CreamDim,
    unfocusedPlaceholderColor = KColors.CreamDim
)
