package com.example.faithquiz

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class AppFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun launchNavigatesToDailyChallenge() {
        composeRule.onNodeWithText("Get Started").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Daily Challenge").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("DailyChallengeQuestion").assertIsDisplayed()
    }

    @Test
    fun settingsExposeAccessibilityControls() {
        composeRule.onNodeWithText("Get Started").performClick()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Text size").assertIsDisplayed()
        composeRule.onNodeWithText("Reduce motion").assertIsDisplayed()
        composeRule.onNodeWithText("Crash reporting").assertIsDisplayed()
    }
}
