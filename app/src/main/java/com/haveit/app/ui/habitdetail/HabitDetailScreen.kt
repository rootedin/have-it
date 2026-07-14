package com.haveit.app.ui.habitdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.OutlinedTextField
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
import com.haveit.app.ui.components.parseHabitColor
import com.haveit.app.ui.theme.FreezeBlue
import com.haveit.app.ui.theme.SuccessGreen
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
    var noteDraft by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.isLoading, state.habit) {
        if (!state.isLoading && state.habit == null) onBack()
    }
    // Seed the note field once from persisted state, then let the user edit freely.
    LaunchedEffect(state.todayNote, state.habit?.id) {
        if (noteDraft == null && state.habit != null) noteDraft = state.todayNote
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

                if (state.freezeCandidate != null && state.freezeCardsAvailable > 0) {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = "놓친 날을 지킬 수 있어요",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${state.freezeCandidate!!.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))}에 체크가 없어요. " +
                                    "프리즈 카드를 쓰면 스트릭이 이어져요. (남은 카드 ${state.freezeCardsAvailable}장)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { viewModel.useFreezeCard(state.freezeCandidate!!) }) {
                                    Text("🛡 프리즈 카드 사용")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                MonthHeatmap(state, viewModel)

                Spacer(Modifier.height(16.dp))
                NoteSection(
                    draft = noteDraft.orEmpty(),
                    onDraftChange = { noteDraft = it },
                    onSave = { viewModel.saveTodayNote(noteDraft.orEmpty()) },
                    pastNotes = state.pastNotes,
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
                    week.forEach { cell -> HeatCellView(cell, Modifier.weight(1f)) }
                    repeat(7 - week.size) { Box(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(8.dp))
            Legend()
        }
    }
}

@Composable
private fun HeatCellView(cell: HeatCell, modifier: Modifier) {
    Box(modifier = modifier.padding(2.dp), contentAlignment = Alignment.Center) {
        if (cell.date == null) {
            Box(Modifier.aspectRatio(1f))
            return@Box
        }
        val bg = when (cell.mark) {
            DayMark.DONE -> SuccessGreen
            DayMark.FROZEN -> FreezeBlue
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
            DayMark.DONE, DayMark.FROZEN -> Color.White
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .border(if (cell.isToday) 2.dp else 1.dp, border, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (cell.mark == DayMark.FROZEN) "🛡" else cell.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun NoteSection(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    pastNotes: List<NoteEntry>,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp)) {
            Text(text = "오늘의 한 줄", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "부담 없이, 남기고 싶을 때만",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text("예: 오늘은 개운했다 😌") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onSave) { Text("저장") }
            }

            if (pastNotes.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "지난 메모",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                pastNotes.forEach { entry ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = entry.date.format(DateTimeFormatter.ofPattern("M/d", Locale.KOREAN)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(40.dp),
                        )
                        Text(text = entry.note, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendItem(SuccessGreen, "완료")
        LegendItem(FreezeBlue, "프리즈")
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
