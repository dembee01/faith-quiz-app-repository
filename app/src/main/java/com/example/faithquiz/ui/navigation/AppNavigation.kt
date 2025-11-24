package com.example.faithquiz.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.faithquiz.ui.view.leaderboard.LeaderboardScreen
import com.example.faithquiz.ui.view.levelselect.LevelSelectScreen
import com.example.faithquiz.ui.view.mainmenu.MainMenuScreen
import com.example.faithquiz.ui.view.quiz.QuizScreen
import com.example.faithquiz.ui.view.results.ResultsScreen
import com.example.faithquiz.ui.view.settings.SettingsScreen
import com.example.faithquiz.ui.view.splash.SplashScreen
import com.example.faithquiz.ui.view.dailychallenge.DailyChallengeScreen
import com.example.faithquiz.ui.view.review.ReviewScreen
import com.example.faithquiz.ui.view.topic.TopicPacksScreen
import com.example.faithquiz.ui.view.topic.TopicQuizScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        
        composable(Screen.MainMenu.route) {
            MainMenuScreen(navController)
        }
        
        composable(Screen.LevelSelect.route) {
            LevelSelectScreen(navController)
        }
        
        composable(
            route = Screen.Quiz.route,
            arguments = listOf(
                navArgument("level") {
                    type = NavType.IntType
                    defaultValue = 1
                },
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "classic"
                }
            )
        ) { backStackEntry ->
            val level = backStackEntry.arguments?.getInt("level") ?: 1
            val mode = backStackEntry.arguments?.getString("mode") ?: "classic"
            // Safety check: ensure level is within valid range
            val validLevel = level.coerceIn(1, 30)
            QuizScreen(navController, validLevel, mode)
        }
        
        composable(Screen.Results.route) {
            ResultsScreen(navController)
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
        
        composable(Screen.Leaderboard.route) {
            LeaderboardScreen(navController)
        }
        
        composable(Screen.DailyChallenge.route) {
            DailyChallengeScreen(navController)
        }

        composable(Screen.Review.route) {
            ReviewScreen(navController)
        }

        composable(Screen.TopicPacks.route) {
            TopicPacksScreen(navController)
        }
        
        composable(
            route = Screen.TopicQuiz.route,
            arguments = listOf(
                navArgument("topic") {
                    type = NavType.StringType
                    defaultValue = "gospels"
                }
            )
        ) { backStackEntry ->
            val topic = backStackEntry.arguments?.getString("topic") ?: "gospels"
            TopicQuizScreen(navController, topic)
        }
    }
}
