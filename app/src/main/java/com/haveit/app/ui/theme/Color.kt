package com.haveit.app.ui.theme

import androidx.compose.ui.graphics.Color

// Brand coral scale (main color). Light theme uses a deeper coral for text/button
// contrast on light surfaces; dark theme uses a brighter coral that reads on charcoal.
val Coral600 = Color(0xFFDD5236) // light-theme primary
val Coral500 = Color(0xFFF2765C) // dark-theme primary / brand splash
val Coral300 = Color(0xFFF7A08D) // lighter accent (dark secondary)
val CoralContainerLight = Color(0xFFFFE1D8)
val CoralOnContainerLight = Color(0xFF7A2A18)
val CoralContainerDark = Color(0xFF522016)
val CoralOnContainerDark = Color(0xFFFFD9CE)
val CoralSplashText = Color(0xFFFFF1EC)

// Dark theme surfaces — neutral charcoal (no color tint)
val DarkBg = Color(0xFF121215)
val DarkSurface = Color(0xFF1D1D22)
val DarkSurfaceHigh = Color(0xFF2A2A31)
val DarkOutline = Color(0xFF3A3A42)

// Light theme surfaces — warm off-white
val LightBg = Color(0xFFFAF6F4)
val LightSurfaceHigh = Color(0xFFF3ECE8)
val LightOutline = Color(0xFFE4D9D3)

// Text
val TextOnDark = Color(0xFFECECEF)
val TextOnDarkDim = Color(0xFF9C9CA6)
val TextOnLight = Color(0xFF241F1D)
val TextOnLightDim = Color(0xFF8A817B)

// Semantic accents (kept distinct from the coral main color)
val SuccessGreen = Color(0xFF44C288)
val FreezeBlue = Color(0xFF5BA8F5)
val FreezeContainerDark = Color(0xFF17324F)
val FreezeOnDark = Color(0xFFABD0F7)
val FreezeContainerLight = Color(0xFFDCEBFB)
val FreezeOnLight = Color(0xFF1A4C79)
val MissedRed = Color(0xFFE06C7D)
