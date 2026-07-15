package com.haveit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt

val HabitColorOptions = listOf(
    "#7C6FF2", "#F27C9C", "#F2A65A", "#44C288",
    "#5BA8F5", "#B06AB3", "#E3B341", "#56CCC5",
)

val HabitEmojiOptions = listOf(
    "💧", "🏃", "📖", "🧘", "💪", "🛏️", "🥗", "✍️",
    "🚶", "🦷", "🎧", "🌿", "☀️", "🧹", "💊", "🎨",
    "🎯", "⏰", "💰", "📝", "🍎", "🥕", "🚴", "🏊",
    "🏋️", "⚽", "🧠", "😴", "🌙", "🌅", "📵", "💻",
    "📚", "🗣️", "🎸", "🙏", "❤️", "🐶", "🌱", "🧺",
    "🧴", "🚗", "🧾", "🎮", "📞", "🧵", "🍳", "🚿",
)

fun parseHabitColor(hex: String?, fallback: Color): Color =
    runCatching { Color(hex!!.toColorInt()) }.getOrDefault(fallback)

@Composable
fun HabitIconBubble(emoji: String, color: Color, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = (size.value * 0.45f).sp)
    }
}
