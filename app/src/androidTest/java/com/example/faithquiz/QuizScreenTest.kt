package com.example.faithquiz

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.faithquiz.ui.view.quiz.QuizScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class QuizScreenTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var navController: TestNavHostController
    
    @Before
    fun setup() {
        hiltRule.inject()
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        
        composeTestRule.setContent {
            QuizScreen(navController = navController, level = 1)
        }
    }
    
    @Test
    fun quizScreen_shouldDisplayLoadingInitially() {
        // Verify loading indicator is shown initially
        composeTestRule.onNodeWithTag("LoadingIndicator").assertExists()
    }
    
    @Test
    fun quizScreen_shouldDisplayBackButton() {
        // Verify back button is present
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
    }
    
    @Test
    fun quizScreen_shouldDisplayLevelText() {
        // Verify level text is displayed
        composeTestRule.onNodeWithText("Level 1").assertExists()
    }
}
