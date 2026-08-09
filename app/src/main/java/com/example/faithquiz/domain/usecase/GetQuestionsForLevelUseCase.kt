package com.example.faithquiz.domain.usecase

import com.example.faithquiz.data.model.Question
import com.example.faithquiz.data.repository.QuestionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetQuestionsForLevelUseCase @Inject constructor(
    private val questionRepository: QuestionRepository
) {
    operator fun invoke(level: Int): Flow<List<Question>> {
        return questionRepository.getQuestionsByLevel(level)
    }
}
