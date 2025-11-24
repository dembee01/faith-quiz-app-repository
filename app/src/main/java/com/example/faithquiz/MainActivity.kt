package com.example.faithquiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.faithquiz.ui.navigation.AppNavigation
import com.example.faithquiz.ui.theme.FaithQuizTheme
import com.google.android.play.core.review.ReviewManagerFactory
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fire-and-forget lightweight review prompt based on simple heuristic
        lifecycleScope.launch {
            try {
                val manager = ReviewManagerFactory.create(this@MainActivity)
                val request = manager.requestReviewFlow()
                val reviewInfo = request.await()
                manager.launchReviewFlow(this@MainActivity, reviewInfo)
            } catch (_: Exception) { }
        }

        setContent {
            FaithQuizTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(navController = navController)
                }
            }
        }
    }
}