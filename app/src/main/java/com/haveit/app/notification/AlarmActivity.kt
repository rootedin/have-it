package com.haveit.app.notification

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haveit.app.ui.theme.HaveItTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The alarm's full-screen face. Launched by [AlarmService]'s full-screen intent, it shows over the
 * lock screen and turns the screen on. It carries no alarm state of its own — the buttons just send
 * commands back to [AlarmService], which owns the sound, vibration, and check-in.
 */
class AlarmActivity : ComponentActivity() {

    /** Set once the user picks 완료 or 닫기, so we stop trapping the screen and let the activity finish. */
    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        // Swallow Back: like a real alarm, this screen can only be left via the on-screen 완료/닫기 buttons.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
        renderFrom(intent)
    }

    /** Home / app-switch while the alarm is still unhandled: pull the alarm screen back to the front. */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (handled) return
        startActivity(
            Intent(this, AlarmActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
                )
                this@AlarmActivity.intent.extras?.let { putExtras(it) }
            },
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        renderFrom(intent)
    }

    private fun renderFrom(intent: Intent) {
        val habitId = intent.getStringExtra(AlarmService.EXTRA_HABIT_ID)
        val name = intent.getStringExtra(AlarmService.EXTRA_HABIT_NAME).orEmpty()
        val icon = intent.getStringExtra(AlarmService.EXTRA_HABIT_ICON).orEmpty()
        val snoozeMinutes = intent.getIntExtra(AlarmService.EXTRA_SNOOZE_MINUTES, 0)
        val canSnooze = intent.getBooleanExtra(AlarmService.EXTRA_CAN_SNOOZE, false)

        setContent {
            HaveItTheme(darkTheme = isSystemInDarkTheme()) {
                AlarmScreen(
                    icon = icon,
                    name = name,
                    canSnooze = canSnooze,
                    snoozeMinutes = snoozeMinutes,
                    onDone = {
                        handled = true
                        sendCommand(AlarmService.ACTION_DONE, habitId)
                        finish()
                    },
                    onDismiss = {
                        handled = true
                        sendCommand(AlarmService.ACTION_STOP, habitId)
                        finish()
                    },
                )
            }
        }
    }

    private fun sendCommand(action: String, habitId: String?) {
        val intent = Intent(this, AlarmService::class.java).apply {
            this.action = action
            if (habitId != null) putExtra(AlarmService.EXTRA_HABIT_ID, habitId)
        }
        startService(intent)
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
private fun AlarmScreen(
    icon: String,
    name: String,
    canSnooze: Boolean,
    snoozeMinutes: Int,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = LocalTime.now().format(DateTimeFormatter.ofPattern("a h:mm")),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (icon.isNotBlank()) {
                    Text(text = icon, fontSize = 72.sp)
                    Spacer(Modifier.height(20.dp))
                }
                Text(
                    text = name.ifBlank { "습관 리마인더" },
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "지금 아주 작게 한 번, 해볼까요?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onDone,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text("완료했어요", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(if (canSnooze) "${snoozeMinutes}분 뒤에 다시 알림" else "닫기")
                }
            }
        }
    }
}
