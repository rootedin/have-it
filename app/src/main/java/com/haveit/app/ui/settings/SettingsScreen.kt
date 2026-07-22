package com.haveit.app.ui.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haveit.app.HaveItApplication
import com.haveit.app.data.backup.BackupManager
import com.haveit.app.data.settings.AppTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenRoutines: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as HaveItApplication
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
    val settings by viewModel.settings.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.importBackup(it) } }

    // The system's own alarm-sound picker: lists whatever tones are on this device (or lets the
    // user add their own) instead of the app bundling and maintaining a fixed set of tones.
    val soundPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.let {
            IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        }
        viewModel.setAlarmSoundUri(uri)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                }
                Text(text = "설정", style = MaterialTheme.typography.headlineSmall)
            }

            settings?.let { current ->
                Spacer(Modifier.height(16.dp))
                SectionTitle("루틴")
                Spacer(Modifier.height(8.dp))
                NavRow(title = "루틴 관리", subtitle = "습관을 원하는 순서대로 묶기", onClick = onOpenRoutines)

                Spacer(Modifier.height(24.dp))
                SectionTitle("테마")
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        ThemeRow("시스템 기본", current.theme == AppTheme.SYSTEM) {
                            viewModel.setTheme(AppTheme.SYSTEM)
                        }
                        ThemeRow("라이트", current.theme == AppTheme.LIGHT) {
                            viewModel.setTheme(AppTheme.LIGHT)
                        }
                        ThemeRow("다크", current.theme == AppTheme.DARK) {
                            viewModel.setTheme(AppTheme.DARK)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                SectionTitle("알림")
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(text = "리마인더 알림", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "습관별 시간은 편집 화면에서 설정해요",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = current.notificationsEnabled,
                            onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val alarmSoundLabel = remember(current.alarmSoundUri) {
                    current.alarmSoundUri
                        ?.let { uriString ->
                            runCatching {
                                RingtoneManager.getRingtone(context, Uri.parse(uriString))?.getTitle(context)
                            }.getOrNull()
                        }
                        ?: "기본 알람음"
                }
                NavRow(
                    title = "알람음",
                    subtitle = alarmSoundLabel,
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                                RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM),
                            )
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                current.alarmSoundUri?.let(Uri::parse),
                            )
                        }
                        soundPickerLauncher.launch(intent)
                    },
                )

                Spacer(Modifier.height(24.dp))
                SectionTitle("데이터")
                Spacer(Modifier.height(8.dp))
                NavRow(
                    title = "보관한 습관",
                    subtitle = "보관된 습관 다시 보기·복원",
                    onClick = onOpenArchive,
                )
                Spacer(Modifier.height(8.dp))
                NavRow(
                    title = "백업 내보내기",
                    subtitle = "모든 데이터를 JSON 파일로 저장",
                    onClick = { exportLauncher.launch(BackupManager.DEFAULT_FILENAME) },
                )
                Spacer(Modifier.height(8.dp))
                NavRow(
                    title = "백업 가져오기",
                    subtitle = "기존 데이터를 덮어써요",
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                )
                Spacer(Modifier.height(8.dp))
                DangerRow(
                    title = "데이터 초기화",
                    subtitle = "모든 습관·기록·루틴을 삭제해요",
                    onClick = { showResetDialog = true },
                )

                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("데이터 초기화") },
            text = { Text("모든 습관, 기록, 루틴이 삭제돼요. 되돌릴 수 없어요.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    viewModel.resetAllData()
                }) {
                    Text("초기화", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DangerRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
