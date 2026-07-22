package com.haveit.app

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.haveit.app.data.settings.AppTheme
import com.haveit.app.ui.navigation.HaveItNavGraph
import com.haveit.app.ui.theme.HaveItTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as HaveItApplication).container
        setContent {
            val settings by container.userSettingsRepository.settings.collectAsState(initial = null)
            val darkTheme = when (settings?.theme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                else -> isSystemInDarkTheme()
            }

            val notificationsEnabled = settings?.notificationsEnabled == true
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { /* result handled by system; reminders no-op silently if denied */ }

            LaunchedEffect(notificationsEnabled) {
                if (notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = getSystemService(AlarmManager::class.java)
                    if (alarmManager?.canScheduleExactAlarms() == false) {
                        // No system dialog for this one (unlike POST_NOTIFICATIONS) — it's a settings
                        // toggle. ReminderScheduler falls back to inexact alarms until it's granted.
                        startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:$packageName"),
                            ),
                        )
                    }
                }
            }

            HaveItTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    HaveItNavGraph()
                }
            }
        }
    }
}
