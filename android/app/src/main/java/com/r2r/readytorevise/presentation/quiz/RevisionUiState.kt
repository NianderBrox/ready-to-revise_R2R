package com.r2r.readytorevise.presentation.quiz

import com.r2r.readytorevise.presentation.base.UiState

data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val nextReviewAt: String? = null,
    val mediaDocumentId: String? = null,
)

data class SessionSummary(
    val reviewedCount: Int,
    val memorizedCount: Int,
    val averageConfidence: Double?,
    val elapsedSeconds: Long,
    val syncFailures: Int = 0,
)

data class RevisionUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selections: Map<String, Int> = emptyMap(),
    val memorizedIds: Set<String> = emptySet(),
    val feedback: Map<String, String> = emptyMap(),
    val confidenceByItem: Map<String, Double> = emptyMap(),
    val finished: Boolean = false,
    val finishing: Boolean = false,
    val summary: SessionSummary? = null,
) : UiState {
    val currentQuestion: QuizQuestion?
        get() = questions.getOrNull(currentIndex)

    val totalQuestions: Int
        get() = questions.size

    val isLastQuestion: Boolean
        get() = currentIndex >= totalQuestions - 1

    fun selectedOptionFor(item: QuizQuestion): Int =
        selections[item.id] ?: -1

    fun isMemorized(item: QuizQuestion): Boolean =
        memorizedIds.contains(item.id)
}
