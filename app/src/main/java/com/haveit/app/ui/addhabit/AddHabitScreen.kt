package com.haveit.app.ui.addhabit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haveit.app.HaveItApplication
import com.haveit.app.data.local.entity.HabitEntity
import com.haveit.app.data.local.entity.HabitFrequency
import com.haveit.app.domain.schedule.HabitSchedule
import com.haveit.app.ui.components.HabitColorOptions
import com.haveit.app.ui.components.HabitEmojiOptions
import com.haveit.app.ui.components.ReminderSettings
import com.haveit.app.ui.components.parseHabitColor

@Composable
fun AddHabitScreen(editingHabitId: String? = null, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as HaveItApplication
    val viewModel: AddHabitViewModel =
        viewModel(factory = AddHabitViewModel.factory(app, editingHabitId))
    val prefill by viewModel.prefill.collectAsState()

    var initialized by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf(HabitEmojiOptions.first()) }
    var colorHex by remember { mutableStateOf(HabitColorOptions.first()) }
    var frequencyName by remember { mutableStateOf(HabitFrequency.DAILY.name) }
    var customDays by remember { mutableStateOf(setOf<Int>()) }
    var trigger by remember { mutableStateOf("") }
    var reminderHour by remember { mutableStateOf<Int?>(null) }
    var reminderMinute by remember { mutableStateOf<Int?>(null) }
    var reminderSnoozeMinutes by remember { mutableStateOf(HabitEntity.DEFAULT_SNOOZE_MINUTES) }
    var reminderSnoozeMaxCount by remember { mutableStateOf(HabitEntity.DEFAULT_SNOOZE_MAX_COUNT) }
    var showEmojiInput by remember { mutableStateOf(false) }
    val customEmoji = emoji.takeIf { it.isNotBlank() && it !in HabitEmojiOptions }

    LaunchedEffect(prefill.loaded) {
        if (prefill.loaded && !initialized) {
            name = prefill.name
            emoji = prefill.emoji.ifBlank { HabitEmojiOptions.first() }
            colorHex = prefill.colorHex.ifBlank { HabitColorOptions.first() }
            frequencyName = prefill.frequency.name
            customDays = prefill.customDays
            trigger = prefill.trigger
            reminderHour = prefill.reminderHour
            reminderMinute = prefill.reminderMinute
            reminderSnoozeMinutes = prefill.reminderSnoozeMinutes
            reminderSnoozeMaxCount = prefill.reminderSnoozeMaxCount
            initialized = true
        }
    }

    val frequency = HabitFrequency.valueOf(frequencyName)
    val canSave = name.isNotBlank() &&
        (frequency != HabitFrequency.CUSTOM_DAYS || customDays.isNotEmpty())

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
                modifier = Modifier.padding(top = 4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                }
                Text(
                    text = if (viewModel.isEditing) "습관 편집" else "새 습관",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Text(
                text = "아주 작게 시작할수록 오래 갑니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("습관 이름") },
                placeholder = { Text("예: 물 한 잔 마시기") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))
            SectionTitle("아이콘")
            Spacer(Modifier.height(10.dp))
            val emojiTiles: List<String?> =
                HabitEmojiOptions + listOfNotNull(customEmoji) + listOf(null)
            emojiTiles.chunked(8).forEach { rowTiles ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    rowTiles.forEach { candidate ->
                        if (candidate == null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape,
                                    )
                                    .clickable { showEmojiInput = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "아이콘 직접 입력",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            val selected = candidate == emoji
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape,
                                    )
                                    .clickable { emoji = candidate },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = candidate, fontSize = 19.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("컬러")
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HabitColorOptions.forEach { candidate ->
                    val color = parseHabitColor(candidate, MaterialTheme.colorScheme.primary)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { colorHex = candidate },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (candidate == colorHex) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("반복")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FrequencyChip("매일", frequency == HabitFrequency.DAILY) {
                    frequencyName = HabitFrequency.DAILY.name
                }
                FrequencyChip("요일 지정", frequency == HabitFrequency.CUSTOM_DAYS) {
                    frequencyName = HabitFrequency.CUSTOM_DAYS.name
                }
                FrequencyChip("주 1회", frequency == HabitFrequency.WEEKLY) {
                    frequencyName = HabitFrequency.WEEKLY.name
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
                                    customDays =
                                        if (selected) customDays - index else customDays + index
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

            Spacer(Modifier.height(24.dp))
            SectionTitle("리마인더 (선택)")
            Spacer(Modifier.height(10.dp))
            ReminderSettings(
                hour = reminderHour,
                minute = reminderMinute,
                snoozeMinutes = reminderSnoozeMinutes,
                snoozeMaxCount = reminderSnoozeMaxCount,
                onChange = { h, m, sMin, sMax ->
                    reminderHour = h
                    reminderMinute = m
                    reminderSnoozeMinutes = sMin
                    reminderSnoozeMaxCount = sMax
                },
            )

            Spacer(Modifier.height(24.dp))
            SectionTitle("트리거 문장 (선택)")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = trigger,
                onValueChange = { trigger = it },
                placeholder = { Text("예: 커피 내린 후 → 스쿼트 10개") },
                supportingText = { Text("이미 하는 행동 뒤에 붙이면 잊지 않아요") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    viewModel.save(
                        name = name,
                        emoji = emoji,
                        colorHex = colorHex,
                        frequency = frequency,
                        customDays = customDays.toList(),
                        trigger = trigger,
                        reminderHour = reminderHour,
                        reminderMinute = reminderMinute,
                        reminderSnoozeMinutes = reminderSnoozeMinutes,
                        reminderSnoozeMaxCount = reminderSnoozeMaxCount,
                        onSaved = onBack,
                    )
                },
                enabled = canSave,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(if (viewModel.isEditing) "변경 사항 저장" else "습관 추가하기")
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showEmojiInput) {
        CustomEmojiDialog(
            initialValue = customEmoji.orEmpty(),
            onConfirm = { value ->
                emoji = value
                showEmojiInput = false
            },
            onDismiss = { showEmojiInput = false },
        )
    }
}

@Composable
private fun CustomEmojiDialog(
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf(initialValue) }
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
                    text = "아이콘 직접 입력",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                )
                OutlinedTextField(
                    value = input,
                    // Caps well short of pasted text while still fitting multi-codepoint
                    // emoji (skin tone modifiers, ZWJ sequences like family/couple emoji).
                    onValueChange = { input = it.take(8) },
                    placeholder = { Text("😀") },
                    supportingText = { Text("키보드의 이모지 버튼으로 입력하세요") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("취소") }
                    TextButton(
                        onClick = { onConfirm(input.trim()) },
                        enabled = input.trim().isNotEmpty(),
                    ) { Text("확인") }
                }
            }
        }
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
private fun FrequencyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}
