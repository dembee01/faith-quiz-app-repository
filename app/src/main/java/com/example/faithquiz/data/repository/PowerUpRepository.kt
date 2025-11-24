package com.example.faithquiz.data.repository

import android.content.Context
import com.example.faithquiz.data.local.GameProgressDao
import com.example.faithquiz.data.model.PowerUp
import com.example.faithquiz.data.model.PowerUpType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PowerUpRepository @Inject constructor(
    private val gameProgressDao: GameProgressDao,
    private val context: Context
) {
    fun getAvailablePowerUps(): Flow<List<PowerUp>> {
        return gameProgressDao.getGameProgress().map { gameProgress ->
            val progress = gameProgress ?: return@map emptyList()
            val powerUps = mutableListOf<PowerUp>()
            
            // Calculate available power-ups based on earned vs used
            val availableCount = progress.powerUpsEarned - progress.powerUpsUsed
            
            // Add 50/50 power-ups (earned every 10 correct answers)
            repeat(availableCount) { index ->
                if (index % 2 == 0) {
                    powerUps.add(PowerUp(type = PowerUpType.FIFTY_FIFTY))
                } else {
                    powerUps.add(PowerUp(type = PowerUpType.SKIP_QUESTION))
                }
            }
            
            powerUps
        }
    }

    suspend fun earnPowerUp() {
        val currentProgress = gameProgressDao.getGameProgress().first()
        val progress = currentProgress ?: return
        
        gameProgressDao.updatePowerUpsEarned(progress.powerUpsEarned + 1)
    }

    suspend fun usePowerUp(type: PowerUpType): Boolean {
        val currentProgress = gameProgressDao.getGameProgress().first()
        val progress = currentProgress ?: return false
        
        val availableCount = progress.powerUpsEarned - progress.powerUpsUsed
        if (availableCount > 0) {
            gameProgressDao.updatePowerUpsUsed(progress.powerUpsUsed + 1)
            return true
        }
        return false
    }

    suspend fun checkAndEarnPowerUp(correctAnswers: Int) {
        // Earn a power-up every 10 correct answers
        if (correctAnswers > 0 && correctAnswers % 10 == 0) {
            earnPowerUp()
        }
    }

    fun getPowerUpCount(): Flow<Int> {
        return gameProgressDao.getGameProgress().map { gameProgress ->
            gameProgress?.let { it.powerUpsEarned - it.powerUpsUsed } ?: 0
        }
    }
}
