package com.haveit.app.ui.routine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haveit.app.HaveItApplication
import com.haveit.app.data.local.entity.RoutineEntity
import com.haveit.app.ui.components.HabitIconBubble
import com.haveit.app.ui.components.parseHabitColor

private data class RoutineDraft(
    val id: String?,
    val name: String,
    val orderedHabitIds: List<String>,
)

@Composable
fun RoutineBuilderScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as HaveItApplication
    val viewModel: RoutineBuilderViewModel = viewModel(factory = RoutineBuilderViewModel.factory(app))
    val state by viewModel.uiState.collectAsState()
    var draft by remember { mutableStateOf<RoutineDraft?>(null) }

    val editing = draft
    if (editing != null) {
        RoutineEditor(
            draft = editing,
            state = state,
            onChange = { draft = it },
            onCancel = { draft = null },
            onSave = {
                viewModel.save(editing.id, editing.name, editing.orderedHabitIds)
                draft = null
            },
        )
        return
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                }
                Text(text = "루틴", style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                text = "습관을 루틴으로 묶어 원하는 순서대로 실천해요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )

            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.routines.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "아직 루틴이 없어요\n첫 루틴을 만들어봐요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    state.routines.forEachIndexed { index, routine ->
                        RoutineCard(
                            routine = routine,
                            memberNames = routine.orderedHabitIds.mapNotNull { state.habitsById[it]?.name },
                            isFirst = index == 0,
                            isLast = index == state.routines.lastIndex,
                            onMoveUp = { viewModel.moveRoutine(routine, -1) },
                            onMoveDown = { viewModel.moveRoutine(routine, 1) },
                            onEdit = {
                                draft = RoutineDraft(
                                    id = routine.id,
                                    name = routine.name,
                                    orderedHabitIds = routine.orderedHabitIds,
                                )
                            },
                            onDelete = { viewModel.delete(routine) },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        draft = RoutineDraft(null, "", emptyList())
                    },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("새 루틴 만들기")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RoutineCard(
    routine: RoutineEntity,
    memberNames: List<String>,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        onClick = onEdit,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "위로")
                }
                IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "아래로")
                }
            }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (memberNames.isEmpty()) "습관 없음"
                    else memberNames.joinToString(" → "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onDelete) {
                Text("삭제", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun RoutineEditor(
    draft: RoutineDraft,
    state: RoutineBuilderUiState,
    onChange: (RoutineDraft) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
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
                IconButton(onClick = onCancel) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "취소")
                }
                Text(
                    text = if (draft.id == null) "새 루틴" else "루틴 편집",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it)) },
                label = { Text("루틴 이름") },
                placeholder = { Text("예: 아침 루틴") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            if (draft.orderedHabitIds.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "실행 순서",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                draft.orderedHabitIds.forEachIndexed { index, id ->
                    val habit = state.habitsById[id] ?: return@forEachIndexed
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.padding(vertical = 3.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(20.dp),
                            )
                            Text(
                                text = "${habit.icon} ${habit.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { onChange(draft.copy(orderedHabitIds = draft.orderedHabitIds.moved(index, -1))) },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "위로")
                            }
                            IconButton(
                                onClick = { onChange(draft.copy(orderedHabitIds = draft.orderedHabitIds.moved(index, 1))) },
                                enabled = index < draft.orderedHabitIds.size - 1,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "아래로")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "습관 선택",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            if (state.activeHabits.isEmpty()) {
                Text(
                    "먼저 습관을 추가해주세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.activeHabits.forEach { habit ->
                val selected = habit.id in draft.orderedHabitIds
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable {
                            val newIds = if (selected) {
                                draft.orderedHabitIds - habit.id
                            } else {
                                draft.orderedHabitIds + habit.id
                            }
                            onChange(draft.copy(orderedHabitIds = newIds))
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HabitIconBubble(
                            emoji = habit.icon,
                            color = parseHabitColor(habit.color, MaterialTheme.colorScheme.primary),
                            size = 36.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "선택됨",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.weight(1f),
                ) { Text("취소") }
                Button(
                    onClick = onSave,
                    enabled = draft.name.isNotBlank() && draft.orderedHabitIds.isNotEmpty(),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.weight(1f),
                ) { Text("저장") }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun <T> List<T>.moved(index: Int, delta: Int): List<T> {
    val target = index + delta
    if (target < 0 || target >= size) return this
    val list = toMutableList()
    val item = list.removeAt(index)
    list.add(target, item)
    return list
}
