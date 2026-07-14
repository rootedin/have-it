package com.haveit.app.ui.navigation

object Destinations {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val ADD_HABIT = "add_habit"
    const val EDIT_HABIT = "edit_habit/{habitId}"
    const val HABIT_DETAIL = "habit_detail/{habitId}"
    const val WEEKLY_REPORT = "weekly_report"
    const val SETTINGS = "settings"
    const val ARCHIVE = "archive"
    const val ROUTINES = "routines"

    fun habitDetail(habitId: String) = "habit_detail/$habitId"
    fun editHabit(habitId: String) = "edit_habit/$habitId"
}
