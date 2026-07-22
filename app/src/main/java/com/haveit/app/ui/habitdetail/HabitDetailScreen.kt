package com.haveit.app.ui.habitdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haveit.app.HaveItApplication
import com.haveit.app.domain.schedule.HabitSchedule
import com.haveit.app.ui.components.HabitIconBubble
import com.haveit.app.ui.components.ReminderSettings
import com.haveit.app.ui.components.parseHabitColor
import com.haveit.app.ui.theme.SuccessGreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HabitDetailScreen(habitId: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val app = LocalContext.current.applicationContext as HaveItApplication
    val viewModel: HabitDetailViewModel =
        viewModel(key = habitId, factory = HabitDetailViewModel.factory(app, habitId))
    val state by viewModel.uiState.collectAsState()

    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoading, state.habit) {
        if (!state.isLoading && state.habit == null) onBack()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .imePadding(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "더보기")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("편집") },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("보관하기") },
                            onClick = {
                                menuOpen = false
                                viewModel.archive(onBack)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("삭제하기", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuOpen = false
                                confirmDelete = true
                            },
                        )
                    }
                }
            }

            state.habit?.let { habit ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HabitIconBubble(
                        emoji = habit.icon,
                        color = parseHabitColor(habit.color, MaterialTheme.colorScheme.primary),
                        size = 56.dp,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(text = habit.name, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = buildString {
                                append(HabitSchedule.label(habit.frequency, habit.customDays))
                                if (habit.reminderHour != null && habit.reminderMinute != null) {
                                    append("  ·  🔔 %02d:%02d".format(habit.reminderHour, habit.reminderMinute))
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                habit.triggerSentence?.takeIf { it.isNotBlank() }?.let { trigger ->
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            text = "⚡ $trigger",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        label = "현재 스트릭",
                        value = if (habit.frequency.name == "WEEKLY") "🔥 ${state.streak}주" else "🔥 ${state.streak}일",
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = state.completionLabel,
                        value = state.completionText,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(16.dp))
                MonthHeatmap(state, viewModel)

                Spacer(Modifier.height(16.dp))
                ReminderCard(
                    hour = habit.reminderHour,
                    minute = habit.reminderMinute,
                    snoozeMinutes = habit.reminderSnoozeMinutes,
                    snoozeMaxCount = habit.reminderSnoozeMaxCount,
                    onChange = { h, m, sMin, sMax ->
                        viewModel.updateReminder(h, m, sMin, sMax)
                    },
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("습관 삭제") },
            text = { Text("기록도 함께 삭제돼요. 되돌릴 수 없어요.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun MonthHeatmap(state: DetailUiState, viewModel: HabitDetailViewModel) {
    var editDate by remember { mutableStateOf<LocalDate?>(null) }

    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.monthLabel,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.previousMonth() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "이전 달")
                }
                IconButton(
                    onClick = { viewModel.nextMonth() },
                    modifier = Modifier.size(32.dp),
                    enabled = state.canGoNextMonth,
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "다음 달")
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                HabitSchedule.DAY_LABELS.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            state.monthCells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        HeatCellView(cell, Modifier.weight(1f), onLongPress = { editDate = it })
                    }
                    repeat(7 - week.size) { Box(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(8.dp))
            Legend()
        }
    }

    val target = editDate
    if (target != null) {
        val isDone = state.monthCells.firstOrNull { it.date == target }?.mark == DayMark.DONE
        AlertDialog(
            onDismissRequest = { editDate = null },
            title = { Text(target.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))) },
            text = { Text(if (isDone) "이 날의 완료 표시를 취소할까요?" else "이 날을 완료로 표시할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleDate(target)
                    editDate = null
                }) {
                    Text(if (isDone) "완료 취소" else "완료로 표시")
                }
            },
            dismissButton = {
                TextButton(onClick = { editDate = null }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun HeatCellView(cell: HeatCell, modifier: Modifier, onLongPress: (LocalDate) -> Unit) {
    Box(modifier = modifier.padding(2.dp), contentAlignment = Alignment.Center) {
        if (cell.date == null) {
            Box(Modifier.aspectRatio(1f))
            return@Box
        }
        val bg = when (cell.mark) {
            DayMark.DONE -> SuccessGreen
            DayMark.MISSED -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
            DayMark.PENDING -> Color.Transparent
            DayMark.OFF -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
        val border = when {
            cell.isToday -> MaterialTheme.colorScheme.primary
            cell.mark == DayMark.MISSED -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            else -> Color.Transparent
        }
        val textColor = when (cell.mark) {
            DayMark.DONE -> Color.White
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .border(if (cell.isToday) 2.dp else 1.dp, border, RoundedCornerShape(8.dp))
                .combinedClickable(onClick = {}, onLongClick = { onLongPress(cell.date) }),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = cell.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun ReminderCard(
    hour: Int?,
    minute: Int?,
    snoozeMinutes: Int,
    snoozeMaxCount: Int,
    onChange: (hour: Int?, minute: Int?, snoozeMinutes: Int, snoozeMaxCount: Int) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp)) {
            Text(text = "리마인더", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "정해진 시간에 알림을 받아요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            ReminderSettings(
                hour = hour,
                minute = minute,
                snoozeMinutes = snoozeMinutes,
                snoozeMaxCount = snoozeMaxCount,
                onChange = onChange,
                // The card is already surface-colored, so give the time tile a distinct tone.
                tileColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendItem(SuccessGreen, "완료")
        LegendItem(MaterialTheme.colorScheme.error, "놓침")
        LegendItem(MaterialTheme.colorScheme.surfaceVariant, "예정 없음")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
