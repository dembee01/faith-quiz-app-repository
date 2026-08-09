package com.example.faithquiz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faithquiz.data.repository.QuestionRepository
import com.example.faithquiz.data.repository.GameProgressRepository
import com.example.faithquiz.data.model.LevelProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LevelSelectViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val gameProgressRepository: GameProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LevelSelectUiState())
    val uiState: StateFlow<LevelSelectUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(isLoading = false, levels = questionRepository.getAvailableLevels())
        }
        viewModelScope.launch {
            runCatching { questionRepository.seedDatabase() }
                .onFailure { error ->
                    _uiState.update { state -> state.copy(error = error.message) }
                }
        }
        viewModelScope.launch {
            gameProgressRepository.getAllLevelProgress()
                .catch { emit(emptyList()) }
                .collect { progress ->
                    _uiState.update { it.copy(levelProgress = progress) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
}

data class LevelSelectUiState(
    val isLoading: Boolean = false,
    val levels: List<Int> = emptyList(),
    val levelProgress: List<LevelProgress> = emptyList(),
    val error: String? = null
)
