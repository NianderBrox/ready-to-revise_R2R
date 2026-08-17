package com.r2r.readytorevise.presentation.quiz

sealed class RevisionItem {

    data class ImageQuestion(
        val imageUri: String
    ) : RevisionItem()

    data class AIQuestion(
        val question: String,
        val options: List<String>,
        val correctAnswer: Int
    ) : RevisionItem()

}