package com.example.faithquiz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faithquiz.data.repository.DataStoreRepository
import com.example.faithquiz.data.repository.GameProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
    private val gameProgressRepository: GameProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                dataStoreRepository.highScore,
                dataStoreRepository.lastLevel,
                dataStoreRepository.totalQuestionsAnswered,
                gameProgressRepository.getGameProgress()
            ) { highScore, lastLevel, totalAnswered, gameProgress ->
                SettingsUiState(
                    highScore = highScore,
                    lastLevel = lastLevel,
                    totalQuestionsAnswered = totalAnswered,
                    currentStreak = gameProgress?.currentStreak ?: 0,
                    longestStreak = gameProgress?.longestStreak ?: 0,
                    levelsUnlocked = gameProgress?.levelsUnlocked ?: 1,
                    powerUpsEarned = gameProgress?.powerUpsEarned ?: 0,
                    dailyChallengesCompleted = gameProgress?.dailyChallengesCompleted ?: 0
                )
            }.collect { settings ->
                _uiState.value = settings
            }
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            // Reset DataStore progress
            dataStoreRepository.resetProgress()
            // Reset GameProgress and LevelProgress
            gameProgressRepository.resetAllProgress()
            // Reload settings
            loadSettings()
        }
    }
}

data class SettingsUiState(
    val highScore: Int = 0,
    val lastLevel: Int = 1,
    val totalQuestionsAnswered: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val levelsUnlocked: Int = 1,
    val powerUpsEarned: Int = 0,
    val dailyChallengesCompleted: Int = 0
)
