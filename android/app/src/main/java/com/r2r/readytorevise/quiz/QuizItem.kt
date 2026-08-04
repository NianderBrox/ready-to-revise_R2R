package com.r2r.readytorevise.presentation.quiz

sealed class QuizItem {

    data class ImageQuestion(
        val imageUri: String
    ) : QuizItem()

    data class AIQuestion(
        val question: String,
        val options: List<String>,
        val correctAnswer: Int
    ) : QuizItem()

}