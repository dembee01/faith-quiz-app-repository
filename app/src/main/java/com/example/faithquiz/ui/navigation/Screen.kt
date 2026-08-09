package com.example.faithquiz.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object MainMenu : Screen("main_menu")
    object LevelSelect : Screen("level_select")
    object Journey : Screen("journey")

    object Quiz : Screen("quiz/{level}?mode={mode}") {
        fun createRoute(level: Int) = "quiz/$level" // default classic mode
        fun createRoute(level: Int, mode: String) = "quiz/$level?mode=$mode"
    }
    object Results : Screen("results")
    object Settings : Screen("settings")
    object Leaderboard : Screen("leaderboard")
    object DailyChallenge : Screen("daily_challenge")
    object Review : Screen("review")
    object TopicPacks : Screen("topic_packs")
    object TopicQuiz : Screen("topic_quiz/{topic}") {
        fun createRoute(topic: String) = "topic_quiz/$topic"
    }
    object GrandCompletion : Screen("grand_completion")

}
