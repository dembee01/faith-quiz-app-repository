package com.example.faithquiz.data.repository

import com.example.faithquiz.data.QuestionBank
import com.example.faithquiz.data.QuestionContent
import com.example.faithquiz.data.local.GameProgressDao
import com.example.faithquiz.data.model.DailyChallenge
import com.example.faithquiz.data.model.GameProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyChallengeRepository @Inject constructor(
    private val gameProgressDao: GameProgressDao
) {
    fun getDailyChallenge(): Flow<DailyChallenge> = gameProgressDao.getGameProgress().map { progress ->
        val today = LocalDate.now().toString()
        DailyChallenge(
            question = DailyChallengeSelector.questionForDate(LocalDate.parse(today)),
            isAvailable = progress?.lastDailyChallengeDate != today,
            isCompleted = progress?.lastDailyChallengeDate == today,
            date = today
        )
    }

    suspend fun completeDailyChallenge() {
        val today = LocalDate.now().toString()
        val progress = gameProgressDao.getGameProgress().first() ?: GameProgress()
        if (progress.lastDailyChallengeDate == today) return

        gameProgressDao.insertGameProgress(
            progress.copy(
                lastDailyChallengeDate = today,
                dailyChallengesCompleted = progress.dailyChallengesCompleted + 1
            )
        )
    }

    fun isDailyChallengeAvailable(): Flow<Boolean> = getDailyChallenge().map { it.isAvailable }

    fun getDailyChallengeProgress(): Flow<Int> = gameProgressDao.getGameProgress().map {
        it?.dailyChallengesCompleted ?: 0
    }

}

internal object DailyChallengeSelector {
    fun questionForDate(date: LocalDate) = QuestionContent.enrich(
        dailyPool[Math.floorMod(date.toEpochDay(), dailyPool.size.toLong()).toInt()]
    )

    private val dailyPool by lazy {
        QuestionBank.getAvailableLevels()
            .flatMap(QuestionBank::getQuestionsForLevel)
            .map(QuestionContent::enrich)
            .distinctBy { it.question.trim().lowercase() }
    }
}
