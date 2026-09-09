package com.nobs.mtglifetracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Player accents follow Magic's five colours plus a multicolour option. Both variants
 * are dark enough to carry white text at the sizes used on the play surface.
 */
data class PlayerPalette(val name: String, val dark: Color, val light: Color)

val PlayerPalettes = listOf(
    PlayerPalette("Plains", Color(0xFF8A7B43), Color(0xFFB8A45E)),
    PlayerPalette("Island", Color(0xFF1F5C8B), Color(0xFF2E7BB0)),
    PlayerPalette("Swamp", Color(0xFF3E3548), Color(0xFF574A66)),
    PlayerPalette("Mountain", Color(0xFF96332B), Color(0xFFC0453A)),
    PlayerPalette("Forest", Color(0xFF2A5D3E), Color(0xFF377952)),
    PlayerPalette("Multicolour", Color(0xFF5B3E8C), Color(0xFF7551AD)),
)

fun paletteFor(index: Int): PlayerPalette = PlayerPalettes[index.mod(PlayerPalettes.size)]

// Neutral chrome: the centre bar and sheets stay out of the way of the player colours.
val SurfaceDark = Color(0xFF121214)
val SurfaceDarkElevated = Color(0xFF1D1D21)
val OnSurfaceDark = Color(0xFFEDEDF0)

val SurfaceLight = Color(0xFFF7F7F8)
val SurfaceLightElevated = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF1A1A1C)

val AccentDark = Color(0xFFCBB26A)
val AccentLight = Color(0xFF7A6420)

val DangerColor = Color(0xFFE05A4F)
