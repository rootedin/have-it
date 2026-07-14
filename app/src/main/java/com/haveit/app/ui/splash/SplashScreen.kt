package com.haveit.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.haveit.app.ui.theme.Coral500
import com.haveit.app.ui.theme.CoralSplashText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Brand loading animation:
 * 0ms "Habit?" -> 1000ms hold -> swaps to "Have It!" in the same spot with a
 * 1 -> 1.06 -> 1 scale bounce, held for ~2s, then hands off to home.
 * Always rendered on the brand coral regardless of app theme.
 */
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(1f) }
    var showHaveIt by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000)
        showHaveIt = true
        launch {
            scale.animateTo(1.06f, animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
            scale.animateTo(1f, animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
        }
        delay(2000) // keep "Have It!" visible for ~2 seconds
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Coral500),
        contentAlignment = Alignment.Center,
    ) {
        // Single centered Text so "Have It!" appears exactly where "Habit?" was.
        Text(
            text = if (showHaveIt) "Have It!" else "Habit?",
            style = MaterialTheme.typography.headlineMedium,
            color = CoralSplashText,
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer(scaleX = scale.value, scaleY = scale.value),
        )
    }
}
