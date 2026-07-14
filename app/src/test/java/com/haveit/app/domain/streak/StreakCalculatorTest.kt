package com.haveit.app.domain.streak

import com.haveit.app.data.local.entity.CheckInEntity
import com.haveit.app.data.local.entity.HabitFrequency
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    // 2024-01-01 is a known Monday; used as a stable anchor instead of LocalDate.now().
    private val monday = LocalDate.of(2024, 1, 1)

    private fun checkIn(date: LocalDate, completed: Boolean = true, usedFreezeCard: Boolean = false) =
        CheckInEntity(
            id = date.toString(),
            habitId = "habit-1",
            epochDay = date.toEpochDay(),
            completed = completed,
            usedFreezeCard = usedFreezeCard,
            note = null,
        )

    @Test
    fun `daily habit counts consecutive completed days`() {
        val today = monday.plusDays(9) // Wed 2024-01-10
        val checkIns = (6..10).map { checkIn(monday.plusDays((it - 1).toLong())) }

        val streak = StreakCalculator.currentStreak(HabitFrequency.DAILY, null, checkIns, today)

        assertEquals(5, streak)
    }

    @Test
    fun `daily habit streak stops at a gap`() {
        val today = monday.plusDays(9) // 2024-01-10
        val checkIns = listOf(
            checkIn(monday.plusDays(9)), // 01-10
            checkIn(monday.plusDays(8)), // 01-09
            // gap at 01-08
            checkIn(monday.plusDays(6)), // 01-07
            checkIn(monday.plusDays(5)), // 01-06
        )

        val streak = StreakCalculator.currentStreak(HabitFrequency.DAILY, null, checkIns, today)

        assertEquals(2, streak)
    }

    @Test
    fun `daily habit does not zero out streak when today is not done yet`() {
        val today = monday.plusDays(9) // 2024-01-10, no entry for today
        val checkIns = listOf(
            checkIn(monday.plusDays(8)), // 01-09
            checkIn(monday.plusDays(7)), // 01-08
            checkIn(monday.plusDays(6)), // 01-07
        )

        val streak = StreakCalculator.currentStreak(HabitFrequency.DAILY, null, checkIns, today)

        assertEquals(3, streak)
    }

    @Test
    fun `custom days habit only counts scheduled days`() {
        val mondayIdx = 0
        val wednesdayIdx = 2
        val fridayIdx = 4
        val today = monday.plusDays(4) // Fri 2024-01-05
        val checkIns = listOf(
            checkIn(monday.plusDays(4)), // Fri
            checkIn(monday.plusDays(2)), // Wed
            checkIn(monday), // Mon
        )

        val streak = StreakCalculator.currentStreak(
            HabitFrequency.CUSTOM_DAYS,
            listOf(mondayIdx, wednesdayIdx, fridayIdx),
            checkIns,
            today,
        )

        assertEquals(3, streak)
    }

    @Test
    fun `custom days habit breaks on a missed scheduled day`() {
        val mondayIdx = 0
        val wednesdayIdx = 2
        val fridayIdx = 4
        val today = monday.plusDays(4) // Fri 2024-01-05
        val checkIns = listOf(
            checkIn(monday.plusDays(4)), // Fri: done
            checkIn(monday.plusDays(2), completed = false), // Wed: missed
            checkIn(monday), // Mon: done, but unreachable past the Wed break
        )

        val streak = StreakCalculator.currentStreak(
            HabitFrequency.CUSTOM_DAYS,
            listOf(mondayIdx, wednesdayIdx, fridayIdx),
            checkIns,
            today,
        )

        assertEquals(1, streak)
    }

    @Test
    fun `freeze card keeps the streak alive without a real completion`() {
        val today = monday.plusDays(9) // 2024-01-10
        val checkIns = listOf(
            checkIn(monday.plusDays(9)), // 01-10 done
            checkIn(monday.plusDays(8), completed = false, usedFreezeCard = true), // 01-09 frozen
            checkIn(monday.plusDays(7)), // 01-08 done
        )

        val streak = StreakCalculator.currentStreak(HabitFrequency.DAILY, null, checkIns, today)

        assertEquals(3, streak)
    }

    @Test
    fun `weekly habit counts consecutive weeks with at least one completion`() {
        val today = monday.plusDays(9) // Wed 2024-01-10, week of Jan 8
        val checkIns = listOf(
            checkIn(monday.plusDays(9)), // this week: Wed Jan 10
            checkIn(monday.plusDays(4)), // previous week: Fri Jan 5
            checkIn(monday.minusDays(7)), // week before that: Mon Dec 25
        )

        val streak = StreakCalculator.currentStreak(HabitFrequency.WEEKLY, null, checkIns, today)

        assertEquals(3, streak)
    }

    @Test
    fun `weekly habit does not break streak while current week is still in progress`() {
        val today = monday.plusDays(9) // Wed 2024-01-10, no completion yet this week
        val checkIns = listOf(
            checkIn(monday.plusDays(4)), // previous week: Fri Jan 5
            checkIn(monday.minusDays(7)), // week before that: Mon Dec 25
        )

        val streak = StreakCalculator.currentStreak(HabitFrequency.WEEKLY, null, checkIns, today)

        assertEquals(2, streak)
    }

    @Test
    fun `weekly habit breaks on a fully missed week`() {
        val today = monday.plusDays(16) // Wed 2024-01-17, week of Jan 15
        val checkIns = listOf(
            checkIn(monday.plusDays(16)), // this week: Wed Jan 17
            // previous week (Jan 8-14) fully missed
            checkIn(monday), // two weeks back: Mon Jan 1
        )

        val streak = StreakCalculator.currentStreak(HabitFrequency.WEEKLY, null, checkIns, today)

        assertEquals(1, streak)
    }
}
