package com.haveit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.haveit.app.data.local.entity.HabitFrequency
import com.haveit.app.domain.schedule.HabitSchedule

/**
 * Reusable repeat-schedule editor: the 매일 / 요일 지정 / 주 1회 chips plus, for 요일 지정, a row of
 * day toggles. Stateless — every change is reported as the full (frequency, customDays) pair through
 * [onChange], so the caller decides whether to buffer it (add form) or persist immediately (detail).
 */
@Composable
fun FrequencyPicker(
    frequency: HabitFrequency,
    customDays: Set<Int>,
    onChange: (HabitFrequency, Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FrequencyOptionChip("매일", frequency == HabitFrequency.DAILY) {
                onChange(HabitFrequency.DAILY, customDays)
            }
            FrequencyOptionChip("요일 지정", frequency == HabitFrequency.CUSTOM_DAYS) {
                onChange(HabitFrequency.CUSTOM_DAYS, customDays)
            }
            FrequencyOptionChip("주 1회", frequency == HabitFrequency.WEEKLY) {
                onChange(HabitFrequency.WEEKLY, customDays)
            }
        }
        if (frequency == HabitFrequency.CUSTOM_DAYS) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HabitSchedule.DAY_LABELS.forEachIndexed { index, label ->
                    val selected = index in customDays
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable {
                                onChange(
                                    frequency,
                                    if (selected) customDays - index else customDays + index,
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FrequencyOptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}
