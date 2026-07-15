package com.haveit.app.ui.weeklyreport

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haveit.app.HaveItApplication
import com.haveit.app.domain.schedule.HabitSchedule
import com.haveit.app.ui.theme.FreezeBlue
import kotlin.math.roundToInt

@Composable
fun WeeklyReportScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as HaveItApplication
    val viewModel: WeeklyReportViewModel = viewModel(factory = WeeklyReportViewModel.factory(app))
    val state by viewModel.uiState.collectAsState()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
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
                Text(text = "리포트", style = MaterialTheme.typography.headlineSmall)
            }

            Spacer(Modifier.height(12.dp))

            if (!state.isLoading && !state.hasAnyHabit) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "아직 기록이 없어요\n습관을 추가하면 리포트가 채워져요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else if (!state.isLoading) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.period == ReportPeriod.WEEK,
                        onClick = { viewModel.selectPeriod(ReportPeriod.WEEK) },
                        label = { Text("주간") },
                    )
                    FilterChip(
                        selected = state.period == ReportPeriod.MONTH,
                        onClick = { viewModel.selectPeriod(ReportPeriod.MONTH) },
                        label = { Text("월간") },
                    )
                }
                Spacer(Modifier.height(12.dp))

                when (state.period) {
                    ReportPeriod.WEEK -> WeekReportContent(
                        state = state,
                        onPrevious = viewModel::previousWeek,
                        onNext = viewModel::nextWeek,
                    )
                    ReportPeriod.MONTH -> MonthReportContent(
                        state = state,
                        onPrevious = viewModel::previousMonth,
                        onNext = viewModel::nextMonth,
                    )
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun WeekReportContent(state: WeeklyReportUiState, onPrevious: () -> Unit, onNext: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (state.isCurrentWeek) "이번 주 완료율" else "주간 완료율",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = state.weekLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "이전 주")
                }
                IconButton(onClick = onNext, modifier = Modifier.size(32.dp), enabled = state.canGoNextWeek) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "다음 주")
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            ) {
                state.bars.forEach { bar ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (bar.hasData) "${(bar.totalRatio * 100).roundToInt()}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        StackedBar(
                            completedRatio = bar.completedRatio,
                            frozenRatio = bar.frozenRatio,
                            isToday = bar.isToday,
                            emptyTrack = !bar.hasData,
                            maxHeight = 118.dp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = bar.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (bar.isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(MaterialTheme.colorScheme.primary, "완료")
                LegendDot(FreezeBlue, "프리즈")
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.isCurrentWeek) "이번 주 평균" else "주간 평균",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = state.weekAveragePercent?.let { "$it%" } ?: "-",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (state.weekFrozenDays > 0) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🛡 프리즈로 지킨 날",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${state.weekFrozenDays}일",
                        style = MaterialTheme.typography.titleMedium,
                        color = FreezeBlue,
                    )
                }
            }
        }
    }

    if (state.weeklyHabitTotal > 0) {
        Spacer(Modifier.height(16.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = "주간 습관", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "주 1회만 채우면 성공",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${state.weeklyHabitDone}/${state.weeklyHabitTotal} 완료",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun MonthReportContent(state: WeeklyReportUiState, onPrevious: () -> Unit, onNext: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "이전 달")
                }
                IconButton(onClick = onNext, modifier = Modifier.size(32.dp), enabled = state.canGoNextMonth) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "다음 달")
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                HabitSchedule.DAY_LABELS.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            state.monthCells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { cell -> MonthCellView(cell, Modifier.weight(1f)) }
                    repeat(7 - week.size) { Box(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(MaterialTheme.colorScheme.primary, "완료")
                LegendDot(FreezeBlue, "프리즈")
                LegendDot(MaterialTheme.colorScheme.error, "놓침")
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.isCurrentMonth) "이번 달 평균" else "월간 평균",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = state.monthAveragePercent?.let { "$it%" } ?: "-",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (state.monthFrozenDays > 0) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🛡 프리즈로 지킨 날",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${state.monthFrozenDays}일",
                        style = MaterialTheme.typography.titleMedium,
                        color = FreezeBlue,
                    )
                }
            }
        }
    }

    if (state.monthWeeklyHabitTotal > 0) {
        Spacer(Modifier.height(16.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = "주간 습관", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "주마다 1회만 채우면 성공",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${state.monthWeeklyHabitDone}/${state.monthWeeklyHabitTotal} 완료",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun MonthCellView(cell: MonthReportCell, modifier: Modifier) {
    Box(modifier = modifier.padding(2.dp), contentAlignment = Alignment.Center) {
        val date = cell.date
        if (date == null) {
            Box(Modifier.aspectRatio(1f))
            return@Box
        }
        val intensity = (0.3f + 0.6f * cell.totalRatio).coerceAtMost(1f)
        val bg = when {
            !cell.hasData -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            cell.totalRatio <= 0f -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
            cell.frozenRatio > cell.completedRatio -> FreezeBlue.copy(alpha = intensity)
            else -> MaterialTheme.colorScheme.primary.copy(alpha = intensity)
        }
        val border = when {
            cell.isToday -> MaterialTheme.colorScheme.primary
            cell.hasData && cell.totalRatio <= 0f -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            else -> Color.Transparent
        }
        val textColor = if (cell.hasData && cell.totalRatio >= 0.5f) Color.White
        else MaterialTheme.colorScheme.onSurfaceVariant
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
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun StackedBar(
    completedRatio: Float,
    frozenRatio: Float,
    isToday: Boolean,
    emptyTrack: Boolean,
    maxHeight: Dp,
) {
    val completedColor = if (isToday) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    Column(
        modifier = Modifier.width(24.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (emptyTrack) {
            Box(
                Modifier
                    .width(24.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            return
        }
        // Frozen segment sits on top of the completed segment.
        if (frozenRatio > 0f) {
            Box(
                Modifier
                    .width(24.dp)
                    .height(maxHeight * frozenRatio)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(FreezeBlue),
            )
        }
        Box(
            Modifier
                .width(24.dp)
                .height((maxHeight * completedRatio).coerceAtLeast(if (completedRatio > 0f) 6.dp else 4.dp))
                .clip(
                    if (frozenRatio > 0f) RoundedCornerShape(0.dp)
                    else RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                )
                .background(if (completedRatio > 0f) completedColor else MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(10.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(3.dp))
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
