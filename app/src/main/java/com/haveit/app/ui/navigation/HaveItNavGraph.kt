package com.haveit.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.haveit.app.ui.addhabit.AddHabitScreen
import com.haveit.app.ui.archive.ArchiveScreen
import com.haveit.app.ui.habitdetail.HabitDetailScreen
import com.haveit.app.ui.home.HomeScreen
import com.haveit.app.ui.routine.RoutineBuilderScreen
import com.haveit.app.ui.settings.SettingsScreen
import com.haveit.app.ui.splash.SplashScreen
import com.haveit.app.ui.weeklyreport.WeeklyReportScreen

@Composable
fun HaveItNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Destinations.SPLASH) {
        composable(Destinations.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(Destinations.HOME) {
            HomeScreen(
                onAddHabit = { navController.navigate(Destinations.ADD_HABIT) },
                onOpenHabit = { habitId -> navController.navigate(Destinations.habitDetail(habitId)) },
                onOpenReport = { navController.navigate(Destinations.WEEKLY_REPORT) },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
            )
        }
        composable(Destinations.ADD_HABIT) {
            AddHabitScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Destinations.EDIT_HABIT,
            arguments = listOf(navArgument("habitId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getString("habitId").orEmpty()
            AddHabitScreen(editingHabitId = habitId, onBack = { navController.popBackStack() })
        }
        composable(
            route = Destinations.HABIT_DETAIL,
            arguments = listOf(navArgument("habitId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getString("habitId").orEmpty()
            HabitDetailScreen(
                habitId = habitId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Destinations.editHabit(habitId)) },
            )
        }
        composable(Destinations.WEEKLY_REPORT) {
            WeeklyReportScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenArchive = { navController.navigate(Destinations.ARCHIVE) },
                onOpenRoutines = { navController.navigate(Destinations.ROUTINES) },
            )
        }
        composable(Destinations.ARCHIVE) {
            ArchiveScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.ROUTINES) {
            RoutineBuilderScreen(onBack = { navController.popBackStack() })
        }
    }
}
