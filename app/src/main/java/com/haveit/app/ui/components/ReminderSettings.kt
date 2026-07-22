package com.haveit.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private val SnoozeIntervalOptions = listOf(5, 10, 15, 30, 60)

/** Sentinel [reminderSnoozeMaxCount] meaning "keep nagging until checked". */
const val UNLIMITED_SNOOZE = Int.MAX_VALUE

/** How many extra reminders to fire before giving up; [UNLIMITED_SNOOZE] never gives up. */
private val SnoozeCountOptions = listOf(UNLIMITED_SNOOZE, 1, 2, 3, 5, 10)

private fun intervalLabel(minutes: Int): String =
    if (minutes % 60 == 0) "${minutes / 60}시간" else "${minutes}분"

private fun countLabel(count: Int): String =
    if (count == UNLIMITED_SNOOZE) "무제한" else "${count}회"

/**
 * Reusable reminder editor: a time tile plus (when a time is set) the snooze interval and
 * "stop nagging after" choices. Holds no reminder state of its own — every change is reported
 * through [onChange] as a full tuple, so the caller decides whether to buffer it (add form) or
 * persist immediately (habit detail).
 *
 * @param tileColor background of the time tile; override when placing the editor on a surface
 * of the same color so the tile still reads as a distinct control.
 */
@Composable
fun ReminderSettings(
    hour: Int?,
    minute: Int?,
    snoozeMinutes: Int,
    snoozeMaxCount: Int,
    onChange: (hour: Int?, minute: Int?, snoozeMinutes: Int, snoozeMaxCount: Int) -> Unit,
    modifier: Modifier = Modifier,
    tileColor: Color = MaterialTheme.colorScheme.surface,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val hasReminder = hour != null && minute != null

    Column(modifier) {
        Surface(
            onClick = { showTimePicker = true },
            shape = MaterialTheme.shapes.medium,
            color = tileColor,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (hasReminder) {
                        "매일 %02d:%02d 알림".format(hour, minute)
                    } else {
                        "알림 시간 설정하기"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                if (hasReminder) {
                    TextButton(onClick = { onChange(null, null, snoozeMinutes, snoozeMaxCount) }) {
                        Text("해제")
                    }
                } else {
                    Text("🔔", fontSize = 18.sp)
                }
            }
        }

        if (hasReminder) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "반복 간격",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SnoozeIntervalOptions.forEach { minutes ->
                    ReminderChip(intervalLabel(minutes), snoozeMinutes == minutes) {
                        onChange(hour, minute, minutes, snoozeMaxCount)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "반복 횟수",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SnoozeCountOptions.forEach { count ->
                    ReminderChip(countLabel(count), snoozeMaxCount == count) {
                        onChange(hour, minute, snoozeMinutes, count)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = snoozeSummary(snoozeMinutes, snoozeMaxCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showTimePicker) {
        ReminderTimeDialog(
            initialHour = hour ?: 9,
            initialMinute = minute ?: 0,
            onConfirm = { h, m ->
                onChange(h, m, snoozeMinutes, snoozeMaxCount)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun ReminderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false,
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "알림 시간",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                )
                TimeInput(state = state)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("취소") }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("확인") }
                }
            }
        }
    }
}

private fun snoozeSummary(intervalMinutes: Int, maxCount: Int): String = when {
    maxCount <= 0 -> "체크할 때까지 딱 한 번만 알려요"
    maxCount == UNLIMITED_SNOOZE -> "체크할 때까지 ${intervalMinutes}분마다 계속 알려요"
    else -> "체크 안 하면 ${intervalMinutes}분마다, 최대 ${maxCount}회까지 다시 알려요"
}
