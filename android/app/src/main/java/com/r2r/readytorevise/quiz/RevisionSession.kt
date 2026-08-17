package com.r2r.readytorevise.presentation.quiz

data class QuestionSession(

    val questionId: String,

    var questionStartedAt: Long = 0L,

    var firstInteractionAt: Long? = null,

    var questionSubmittedAt: Long = 0L,

    var correct: Boolean = false,

    var answerChanges: Int = 0

)