package com.example.faithquiz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faithquiz.data.repository.QuestionRepository
import com.example.faithquiz.data.repository.GameProgressRepository
import com.example.faithquiz.data.model.LevelProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class LevelSelectViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val gameProgressRepository: GameProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LevelSelectUiState())
    val uiState: StateFlow<LevelSelectUiState> = _uiState.asStateFlow()

    init {
        android.util.Log.d("LevelSelectViewModel", "Initializing LevelSelectViewModel...")
        // Show levels immediately without database operations for testing
        android.util.Log.d("LevelSelectViewModel", "Using immediate fallback levels for testing")
        loadFallbackLevels()
    }

    private fun loadLevels() {
        viewModelScope.launch {
            try {
                android.util.Log.d("LevelSelectViewModel", "Loading levels...")
                
                // Use timeout to prevent hanging
                withTimeout(10000) { // 10 second timeout for database operations
                    questionRepository.getAllLevels()
                        .onStart { 
                            android.util.Log.d("LevelSelectViewModel", "Starting to load levels")
                            _uiState.update { it.copy(isLoading = true) } 
                        }
                        .onEach { levels ->
                            android.util.Log.d("LevelSelectViewModel", "Loaded ${levels.size} levels: $levels")
                            if (levels.isEmpty()) {
                                // If no levels found, try to seed the database first
                                android.util.Log.d("LevelSelectViewModel", "No levels found, attempting to seed database...")
                                try {
                                    // Add timeout to database seeding
                                    withTimeout(15000) { // 15 second timeout for seeding
                                        questionRepository.seedDatabase()
                                    }
                                    android.util.Log.d("LevelSelectViewModel", "Database seeding completed, retrying level load...")
                                    // After seeding, try to load levels again
                                    questionRepository.getAllLevels().collect { seededLevels ->
                                        android.util.Log.d("LevelSelectViewModel", "After seeding, loaded ${seededLevels.size} levels")
                                        if (seededLevels.isNotEmpty()) {
                                            _uiState.update { 
                                                it.copy(
                                                    isLoading = false,
                                                    levels = seededLevels
                                                )
                                            }
                                        } else {
                                            // If still no levels, use fallback
                                            android.util.Log.d("LevelSelectViewModel", "Still no levels after seeding, using fallback")
                                            val defaultLevels = (1..30).toList()
                                            _uiState.update { 
                                                it.copy(
                                                    isLoading = false,
                                                    levels = defaultLevels
                                                )
                                            }
                                        }
                                    }
                                } catch (seedingError: Exception) {
                                    android.util.Log.e("LevelSelectViewModel", "Error seeding database", seedingError)
                                    // Use fallback if seeding fails
                                    val defaultLevels = (1..30).toList()
                                    _uiState.update { 
                                        it.copy(
                                            isLoading = false,
                                            levels = defaultLevels
                                        )
                                    }
                                }
                            } else {
                                _uiState.update { 
                                    it.copy(
                                        isLoading = false,
                                        levels = levels
                                    )
                                }
                            }
                        }
                        .catch { error ->
                            android.util.Log.e("LevelSelectViewModel", "Error loading levels", error)
                            // Try to seed database on error
                            try {
                                android.util.Log.d("LevelSelectViewModel", "Attempting to seed database after error...")
                                withTimeout(15000) { // 15 second timeout for seeding
                                    questionRepository.seedDatabase()
                                }
                                val defaultLevels = (1..30).toList()
                                _uiState.update { 
                                    it.copy(
                                        isLoading = false,
                                        levels = defaultLevels,
                                        error = null
                                    )
                                }
                            } catch (seedingError: Exception) {
                                android.util.Log.e("LevelSelectViewModel", "Error seeding database after level load error", seedingError)
                                // Final fallback
                                val defaultLevels = (1..30).toList()
                                _uiState.update { 
                                    it.copy(
                                        isLoading = false,
                                        levels = defaultLevels,
                                        error = null
                                    )
                                }
                            }
                        }
                        .collect()
                }
            } catch (e: Exception) {
                android.util.Log.e("LevelSelectViewModel", "Unexpected error in loadLevels", e)
                // Try to seed database on timeout
                try {
                    android.util.Log.d("LevelSelectViewModel", "Attempting to seed database after timeout...")
                    withTimeout(15000) { // 15 second timeout for seeding
                        questionRepository.seedDatabase()
                    }
                } catch (seedingError: Exception) {
                    android.util.Log.e("LevelSelectViewModel", "Error seeding database after timeout", seedingError)
                }
                // Final fallback
                val defaultLevels = (1..30).toList()
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        levels = defaultLevels,
                        error = null
                    )
                }
            }
        }
    }

    private fun loadLevelProgress() {
        viewModelScope.launch {
            try {
                android.util.Log.d("LevelSelectViewModel", "Loading level progress...")
                
                // Use timeout to prevent hanging
                withTimeout(3000) { // 3 second timeout
                    gameProgressRepository.getAllLevelProgress()
                        .onEach { levelProgress ->
                            android.util.Log.d("LevelSelectViewModel", "Loaded ${levelProgress.size} level progress entries")
                            _uiState.update { 
                                it.copy(
                                    levelProgress = levelProgress
                                )
                            }
                        }
                        .catch { error ->
                            android.util.Log.e("LevelSelectViewModel", "Error loading level progress", error)
                            // Don't set error state, just continue with empty progress
                            _uiState.update { 
                                it.copy(
                                    levelProgress = emptyList()
                                )
                            }
                        }
                        .collect()
                }
            } catch (e: Exception) {
                android.util.Log.e("LevelSelectViewModel", "Unexpected error in loadLevelProgress", e)
                // Continue with empty progress
                _uiState.update { 
                    it.copy(
                        levelProgress = emptyList()
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun loadFallbackLevels() {
        android.util.Log.d("LevelSelectViewModel", "Loading fallback levels...")
        val defaultLevels = (1..30).toList()
        _uiState.update { 
            it.copy(
                isLoading = false,
                levels = defaultLevels,
                error = null
            )
        }
    }
    
    private fun loadLevelsWithFallback() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                android.util.Log.d("LevelSelectViewModel", "Loading levels with fallback...")
                
                // Use a shorter timeout for better responsiveness
                withTimeout(5000) { // 5 second timeout
                    questionRepository.getAllLevels()
                        .onStart { 
                            android.util.Log.d("LevelSelectViewModel", "Starting to load levels")
                        }
                        .onEach { levels ->
                            android.util.Log.d("LevelSelectViewModel", "Loaded ${levels.size} levels: $levels")
                            if (levels.isNotEmpty()) {
                                _uiState.update { 
                                    it.copy(
                                        isLoading = false,
                                        levels = levels
                                    )
                                }
                            } else {
                                // If no levels found, use fallback immediately
                                android.util.Log.d("LevelSelectViewModel", "No levels found, using fallback")
                                loadFallbackLevels()
                            }
                        }
                        .catch { error ->
                            android.util.Log.e("LevelSelectViewModel", "Error loading levels", error)
                            loadFallbackLevels()
                        }
                        .collect()
                }
            } catch (e: Exception) {
                android.util.Log.e("LevelSelectViewModel", "Unexpected error in loadLevelsWithFallback", e)
                loadFallbackLevels()
            }
        }
    }
}

data class LevelSelectUiState(
    val isLoading: Boolean = false,
    val levels: List<Int> = emptyList(),
    val levelProgress: List<LevelProgress> = emptyList(),
    val error: String? = null
)
