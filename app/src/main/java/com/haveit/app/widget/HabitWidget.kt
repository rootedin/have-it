package com.haveit.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.haveit.app.HaveItApplication
import com.haveit.app.data.local.entity.CheckInEntity
import com.haveit.app.data.local.entity.HabitEntity
import com.haveit.app.domain.schedule.HabitSchedule
import java.time.LocalDate
import kotlinx.coroutines.flow.first

private val HABIT_ID_KEY = ActionParameters.Key<String>("habit_id")

class HabitWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as HaveItApplication
        val today = LocalDate.now()
        val habits = app.container.habitRepository.observeActiveHabits().first()
            .filter { HabitSchedule.isScheduledOn(it.frequency, it.customDays, today) }
        val checkedIds = app.container.checkInRepository.observeAll().first()
            .filter { it.epochDay == today.toEpochDay() && it.completed }
            .map { it.habitId }
            .toSet()

        provideContent {
            WidgetBody(habits, checkedIds)
        }
    }

    @Composable
    private fun WidgetBody(habits: List<HabitEntity>, checkedIds: Set<String>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF1D1D22))
                .cornerRadius(20.dp)
                .padding(14.dp),
        ) {
            val doneCount = habits.count { it.id in checkedIds }
            Text(
                text = "오늘의 습관  $doneCount/${habits.size}",
                style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFFECECEF)), fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.height(8.dp))
            if (habits.isEmpty()) {
                Text(
                    text = "오늘 예정된 습관이 없어요",
                    style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF9C9CA6))),
                )
            } else {
                habits.take(5).forEach { habit ->
                    WidgetRow(habit, habit.id in checkedIds)
                    Spacer(GlanceModifier.height(6.dp))
                }
            }
        }
    }

    @Composable
    private fun WidgetRow(habit: HabitEntity, checked: Boolean) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(Color(0xFF2A2A31))
                .cornerRadius(12.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clickable(actionRunCallback<ToggleHabitAction>(actionParametersOf(HABIT_ID_KEY to habit.id))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (checked) "✅" else "⬜",
                style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White)),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = "${habit.icon} ${habit.name}",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(
                        if (checked) Color(0xFFF2765C) else Color(0xFFECECEF),
                    ),
                ),
            )
        }
    }

    companion object {
        suspend fun refresh(context: Context) {
            HabitWidget().updateAll(context)
        }
    }
}

class ToggleHabitAction : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val habitId = parameters[HABIT_ID_KEY] ?: return
        val app = context.applicationContext as HaveItApplication
        val epochDay = LocalDate.now().toEpochDay()
        val existing = app.container.checkInRepository.getForHabitOnDay(habitId, epochDay)
        if (existing?.completed == true) {
            app.container.checkInRepository.deleteForDay(habitId, epochDay)
        } else {
            app.container.checkInRepository.upsert(
                CheckInEntity(
                    id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                    habitId = habitId,
                    epochDay = epochDay,
                    completed = true,
                    note = existing?.note,
                ),
            )
        }
        HabitWidget().updateAll(context)
    }
}
