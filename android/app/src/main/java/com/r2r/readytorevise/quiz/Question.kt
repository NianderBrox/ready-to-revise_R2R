package com.r2r.readytorevise.presentation.quiz

data class Question(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val aiGenerated: Boolean = true
)