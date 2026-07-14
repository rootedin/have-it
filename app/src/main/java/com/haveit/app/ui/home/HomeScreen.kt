package com.haveit.app.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haveit.app.HaveItApplication
import com.haveit.app.domain.schedule.HabitSchedule
import com.haveit.app.ui.components.HabitIconBubble
import com.haveit.app.ui.components.parseHabitColor
import com.haveit.app.ui.theme.FreezeBlue
import com.haveit.app.ui.theme.SuccessGreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    onAddHabit: () -> Unit,
    onOpenHabit: (String) -> Unit,
    onOpenReport: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as HaveItApplication
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddHabit,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "습관 추가")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            HomeHeader(date = state.date, onOpenReport = onOpenReport, onOpenSettings = onOpenSettings)

            when {
                state.isLoading -> Box(Modifier.fillMaxSize())
                state.isEmpty -> EmptyState(onAddHabit = onAddHabit)
                else -> {
                    Spacer(Modifier.height(4.dp))
                    TodayProgressCard(
                        done = state.doneCount,
                        total = state.totalCount,
                        freezeCards = state.freezeCardsAvailable,
                        onClick = onOpenReport,
                    )
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        state.sections.forEach { section ->
                            if (section.title != null) {
                                item(key = "header_${section.title}") {
                                    Text(
                                        text = section.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                                    )
                                }
                            }
                            items(section.items.size, key = { section.items[it].habit.id }) { idx ->
                                val item = section.items[idx]
                                HabitRow(
                                    item = item,
                                    onToggle = { viewModel.toggleToday(item) },
                                    onClick = { onOpenHabit(item.habit.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(date: LocalDate, onOpenReport: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = "오늘의 습관", style = MaterialTheme.typography.headlineMedium)
        }
        IconButton(onClick = onOpenReport) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = "주간 리포트",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "설정",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TodayProgressCard(done: Int, total: Int, freezeCards: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressRing(done = done, total = total)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = when {
                        total == 0 -> ""
                        done == 0 -> "오늘의 첫 체크를 기다려요"
                        done < total -> "좋아요, ${total - done}개 남았어요"
                        else -> "오늘 전부 해냈어요! 🎉"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "프리즈 카드 🛡 ${freezeCards}장 보유",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(done: Int, total: Int) {
    val fraction by animateFloatAsState(
        targetValue = if (total == 0) 0f else done.toFloat() / total,
        label = "progress",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(64.dp)) {
            val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
            )
            if (fraction > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    style = stroke,
                )
            }
        }
        Text(text = "$done/$total", style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun HabitRow(item: TodayHabitUi, onToggle: () -> Unit, onClick: () -> Unit) {
    val habitColor = parseHabitColor(item.habit.color, MaterialTheme.colorScheme.primary)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HabitIconBubble(emoji = item.habit.icon, color = habitColor, size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.habit.triggerSentence?.takeIf { it.isNotBlank() }
                        ?: HabitSchedule.label(item.habit.frequency, item.habit.customDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val streakText = when {
                    item.streak <= 0 -> null
                    item.isWeekly -> "🔥 ${item.streak}주 연속"
                    else -> "🔥 ${item.streak}일 연속"
                }
                val weeklyDoneHint =
                    if (item.isWeekly && item.checked && !item.doneToday) "이번 주 완료" else null
                if (streakText != null || weeklyDoneHint != null) {
                    Spacer(Modifier.height(2.dp))
                    Row {
                        if (streakText != null) {
                            Text(
                                text = streakText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (streakText != null && weeklyDoneHint != null) {
                            Text(
                                text = "  ·  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (weeklyDoneHint != null) {
                            Text(
                                text = weeklyDoneHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            CheckCircle(
                checked = item.checked,
                frozenOnly = item.frozenToday && !item.doneToday,
                onToggle = onToggle,
            )
        }
    }
}

@Composable
private fun CheckCircle(checked: Boolean, frozenOnly: Boolean, onToggle: () -> Unit) {
    val fillColor by animateColorAsState(
        targetValue = when {
            frozenOnly -> FreezeBlue
            checked -> SuccessGreen
            else -> Color.Transparent
        },
        label = "checkFill",
    )
    val borderColor = when {
        frozenOnly -> FreezeBlue
        checked -> SuccessGreen
        else -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(fillColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        when {
            frozenOnly -> Text(text = "🛡", fontSize = 14.sp)
            checked -> Icon(
                Icons.Default.Check,
                contentDescription = "완료",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun EmptyState(onAddHabit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 72.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🌱", fontSize = 44.sp)
            Spacer(Modifier.height(12.dp))
            Text(text = "아직 습관이 없어요", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "실패해도 괜찮아요.\n아주 작은 것부터 시작해봐요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAddHabit, shape = MaterialTheme.shapes.large) {
                Text("첫 습관 만들기")
            }
        }
    }
}
