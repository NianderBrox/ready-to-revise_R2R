package com.r2r.readytorevise.presentation.quiz

import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import com.r2r.readytorevise.data.remote.dueBeforeTodayISO
import com.r2r.readytorevise.data.remote.dto.CreateReviewRequestDto
import com.r2r.readytorevise.data.remote.dto.SelfGradeReviewDto
import com.r2r.readytorevise.domain.repository.RecommendationsRepository
import com.r2r.readytorevise.domain.repository.ReviewsRepository
import com.r2r.readytorevise.domain.repository.StudyItemsRepository
import com.r2r.readytorevise.presentation.base.BaseViewModel
import kotlinx.coroutines.launch
import java.util.UUID

class RevisionViewModel(
    private val studyItemsRepository: StudyItemsRepository,
    private val recommendationsRepository: RecommendationsRepository,
    private val reviewsRepository: ReviewsRepository,
) : BaseViewModel<RevisionUiState, RevisionEvent, RevisionEffect>(RevisionUiState()) {

    private val sessionId: String = UUID.randomUUID().toString()

    private val sessionStartedAtMs: Long = SystemClock.elapsedRealtime()

    private val trackers = mutableMapOf<String, QuestionTiming>()

    private class QuestionTiming {
        var startedAtMs: Long = 0L
        var firstInteractionMs: Long? = null
        var answerChanges: Int = 0
    }

    init {
        load()
    }

    override fun onEvent(event: RevisionEvent) {
        when (event) {
            RevisionEvent.RetryLoad -> load()
            is RevisionEvent.SelectOption -> selectOption(event.index)
            RevisionEvent.NextClicked -> moveNext()
            RevisionEvent.BackClicked -> moveBack()
            RevisionEvent.MemorizeSwiped -> toggleMemorized()
            RevisionEvent.FinishConfirmed -> finishSession()
            RevisionEvent.ExitConfirmed -> exitSession()
        }
    }

    private fun load() {
        updateState {
            copy(loading = true, error = null, finished = false, summary = null)
        }

        viewModelScope.launch {
            val dueBefore = dueBeforeTodayISO()
            val result = recommendationsRepository.getRecommendations(
                limit = SESSION_LIMIT,
                dueBefore = dueBefore,
            )

            val payload = result.getOrNull()

            if (payload == null) {
                updateState {
                    copy(
                        loading = false,
                        error = result.exceptionOrNull()?.message
                            ?: "Couldn't load your revision set.",
                    )
                }
                return@launch
            }

            val questions = payload.items.mapNotNull { item ->
                val options = item.options

                if (options.isNullOrEmpty()) {
                    return@mapNotNull null
                }

                QuizQuestion(
                    id = item.studyItemId,
                    question = item.title?.takeIf { it.isNotBlank() }
                        ?: "Untitled question",
                    options = options,
                    nextReviewAt = item.nextReviewAt,
                    mediaDocumentId = item.mediaDocumentId,
                )
            }.filter { it.options.isNotEmpty() }

            if (questions.isEmpty()) {
                resolveEmptyState()
                return@launch
            }

            val now = SystemClock.elapsedRealtime()

            trackers.clear()

            questions.forEach { question ->
                trackers[question.id] = QuestionTiming().apply { startedAtMs = now }
            }

            updateState {
                copy(
                    loading = false,
                    questions = questions,
                    currentIndex = 0,
                    selections = emptyMap(),
                    memorizedIds = emptySet(),
                    feedback = emptyMap(),
                    confidenceByItem = emptyMap(),
                )
            }
        }
    }

    private suspend fun resolveEmptyState() {
        val anyResult = studyItemsRepository.getStudyItems(type = "QUESTION")

        val hasAny = anyResult.getOrNull()?.isNotEmpty() == true

        updateState {
            copy(
                loading = false,
                error = if (hasAny) {
                    "All caught up! Nothing is due for review today."
                } else {
                    "No questions yet. Upload a document to generate some!"
                },
            )
        }
    }

    private fun selectOption(index: Int) {
        val state = currentState
        val question = state.currentQuestion ?: return

        if (state.finished || state.finishing || state.isMemorized(question)) {
            return
        }

        if (index < 0 || index >= question.options.size) {
            return
        }

        val timing = trackerFor(question.id)
        val now = SystemClock.elapsedRealtime()

        if (timing.firstInteractionMs == null) {
            timing.firstInteractionMs = now
        }

        val previous = state.selectedOptionFor(question)

        if (previous != -1 && previous != index) {
            timing.answerChanges += 1
        }

        updateState {
            copy(selections = selections + (question.id to index))
        }
    }

    private fun moveNext() {
        val state = currentState

        if (!state.finished && !state.isLastQuestion) {
            updateState { copy(currentIndex = currentIndex + 1) }
        }
    }

    private fun moveBack() {
        updateState {
            if (currentIndex > 0) copy(currentIndex = currentIndex - 1) else this
        }
    }

    private fun toggleMemorized() {
        val state = currentState
        val question = state.currentQuestion ?: return

        if (state.finished || state.finishing) {
            return
        }

        val marking = !state.memorizedIds.contains(question.id)

        updateState {
            copy(
                memorizedIds = if (marking) memorizedIds + question.id
                else memorizedIds - question.id,
                selections = if (marking) selections - question.id else selections,
            )
        }
    }

    private fun finishSession() {
        val state = currentState

        if (state.finished || state.finishing) {
            return
        }

        updateState { copy(finishing = true) }

        viewModelScope.launch {
            val finishNow = SystemClock.elapsedRealtime()

            val sessionDurationMinutes =
                ((finishNow - sessionStartedAtMs) / 60_000).toInt()

            var answeredOk = 0

            var marksOk = 0

            var failures = 0

            state.questions.forEachIndexed { index, question ->
                val selection = state.selections[question.id] ?: -1

                if (selection == -1 || state.memorizedIds.contains(question.id)) {
                    return@forEachIndexed
                }

                val timing = trackers[question.id]

                val request = CreateReviewRequestDto(
                    studyItemId = question.id,
                    selectedOptionIndex = selection,
                    responseTimeMs = elapsedSince(timing?.startedAtMs, finishNow),
                    hesitationMs = timing?.firstInteractionMs
                        ?.let { elapsedSince(timing.startedAtMs, it) },
                    answerChanges = timing?.answerChanges ?: 0,
                    sessionId = sessionId,
                    sessionDurationMinutes = sessionDurationMinutes,
                    questionPositionInSession = index + 1,
                )

                reviewsRepository.submit(request)
                    .onSuccess { review ->
                        answeredOk += 1

                        updateState {
                            copy(
                                confidenceByItem = confidenceByItem +
                                    (question.id to (review.confidenceScore ?: 0.0)),
                            )
                        }
                    }
                    .onFailure { failures += 1 }
            }

            state.questions.forEachIndexed { index, question ->
                val id = question.id

                if (state.selections[id] != null && state.selections[id] != -1) {
                    return@forEachIndexed
                }

                if (!state.memorizedIds.contains(id)) {
                    return@forEachIndexed
                }

                val timing = trackers[id]

                val request = SelfGradeReviewDto(
                    studyItemId = id,
                    responseTimeMs = elapsedSince(timing?.startedAtMs, finishNow),
                    sessionId = sessionId,
                    sessionDurationMinutes = sessionDurationMinutes,
                    questionPositionInSession = index + 1,
                )

                reviewsRepository.selfGrade(request)
                    .onSuccess { marksOk += 1 }
                    .onFailure { failures += 1 }
            }

            val confidences = state.confidenceByItem.values

            val summary = SessionSummary(
                reviewedCount = answeredOk,
                memorizedCount = marksOk,
                averageConfidence = if (confidences.isEmpty()) null
                else confidences.average(),
                elapsedSeconds = (SystemClock.elapsedRealtime() -
                    sessionStartedAtMs) / 1000,
                syncFailures = failures,
            )

            updateState {
                copy(finishing = false, finished = true, summary = summary)
            }
        }
    }

    private fun exitSession() {
        viewModelScope.launch {
            sendEffect(RevisionEffect.NavigateBack)
        }
    }

    private fun trackerFor(questionId: String): QuestionTiming {
        return trackers.getOrPut(questionId) {
            QuestionTiming().apply { startedAtMs = SystemClock.elapsedRealtime() }
        }
    }

    private fun elapsedSince(startedAtMs: Long?, nowMs: Long): Int? {
        if (startedAtMs == null) {
            return null
        }

        val elapsed = nowMs - startedAtMs

        return if (elapsed <= 0L) 0 else elapsed.toInt()
    }

    private companion object {
        const val SESSION_LIMIT = 20
    }
}
