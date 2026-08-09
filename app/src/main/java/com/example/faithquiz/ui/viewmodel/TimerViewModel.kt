package com.example.faithquiz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor() : ViewModel() {

    private val _currentQuestionTime = MutableStateFlow(0L)
    val currentQuestionTime: StateFlow<Long> = _currentQuestionTime.asStateFlow()

    private val _totalLevelTime = MutableStateFlow(0L)
    val totalLevelTime: StateFlow<Long> = _totalLevelTime.asStateFlow()

    private var timerJob: Job? = null
    private var isPaused = false

    fun startTimer() {
        if (timerJob?.isActive == true && !isPaused) return
        
        isPaused = false
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _currentQuestionTime.update { it + 1 }
                _totalLevelTime.update { it + 1 }
            }
        }
    }

    fun pauseTimer() {
        isPaused = true
        timerJob?.cancel()
    }

    fun resetQuestionTime() {
        _currentQuestionTime.value = 0L
        // Ensure timer is running if it was supposed to be
        if (!isPaused && (timerJob == null || !timerJob!!.isActive)) {
            startTimer()
        }
    }
    
    fun setTotalTime(time: Long) {
        _totalLevelTime.value = time
    }
    
    fun resetAll() {
        _currentQuestionTime.value = 0L
        _totalLevelTime.value = 0L
        pauseTimer()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
