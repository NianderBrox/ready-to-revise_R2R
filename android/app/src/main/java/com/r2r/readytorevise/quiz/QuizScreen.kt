package com.r2r.readytorevise.presentation.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.r2r.readytorevise.presentation.upload.UploadViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape

data class Question(
    val id: String,
    val question: String,
    val options: List<String> = emptyList(),
    val correctAnswer: Int,
    val aiGenerated: Boolean = false
)

@Composable
fun QuizScreen(
    uploadViewModel: UploadViewModel,
    onBackClick: () -> Unit
) {
    val uploadedImages = uploadViewModel.images
    var elapsedSeconds by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    var showExitDialog by remember {
        mutableStateOf(false)
    }

    BackHandler {
        showExitDialog = true
    }

    var currentQuestion by remember {
        mutableIntStateOf(0)
    }
    val offsetX = remember {
        Animatable(0f)
    }

    val scope = rememberCoroutineScope()

    val density = LocalDensity.current
    var showMemorized by remember {
        mutableStateOf(false)
    }

    val aiQuestions = listOf(
        Question(
            id = "ai1",
            question = "What is the main purpose of an Operating System?",
            options = listOf(
                "Manage hardware resources",
                "Create websites",
                "Store only images",
                "Design applications"
            ),
            correctAnswer = 0,
            aiGenerated = true
        ),
        Question(
            id = "ai2",
            question = "Which memory is temporary?",
            options = listOf(
                "ROM",
                "RAM",
                "SSD",
                "Hard Disk"
            ),
            correctAnswer = 1,
            aiGenerated = true
        )
    )

    val totalQuestions = uploadedImages.size + aiQuestions.size

    val selectedAnswers = remember(totalQuestions) {
        mutableStateListOf<Int>().apply {
            repeat(totalQuestions) {
                add(-1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Question ${currentQuestion + 1} / $totalQuestions",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = String.format(
                "Time: %02d:%02d",
                elapsedSeconds / 60,
                elapsedSeconds % 60
            ),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = {
                if (totalQuestions > 0) {
                    (currentQuestion + 1).toFloat() / totalQuestions.toFloat()
                } else {
                    0f
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    // Memorized green background card
                    if (showMemorized) {

                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {

                                Text(
                                    text = "✓ Memorized",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge
                                )

                            }

                        }

                    }
                    // Swipeable Question Card
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = offsetX.value
                            }
                            .pointerInput(currentQuestion) {
                                val swipeThresholdPx = with(density) { -180.dp.toPx() }
                                val maxSwipePx = with(density) { -400.dp.toPx() }

                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        scope.launch {
                                            if (offsetX.value <= swipeThresholdPx) {

                                                showMemorized = true

                                                offsetX.animateTo(
                                                    targetValue = maxSwipePx,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    )
                                                )

                                                delay(700)

                                                showMemorized = false

                                                if (currentQuestion == totalQuestions - 1) {
                                                    showExitDialog = true
                                                } else {
                                                    currentQuestion++
                                                }

                                                offsetX.snapTo(0f)
                                            } else {
                                                // Bounce back to starting position
                                                offsetX.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        // Apply resistance (0.5f) and allow ONLY left swipes (negative offsetX)
                                        val resistance = 0.5f
                                        val newOffset = (offsetX.value + dragAmount * resistance)
                                            .coerceIn(maxSwipePx, 0f)

                                        scope.launch {
                                            offsetX.snapTo(newOffset)
                                        }
                                    }
                                )
                            }

                    ) {
                        Column(
                            modifier = Modifier.padding(15.dp)
                        ) {
                            if (currentQuestion < uploadedImages.size) {
                                AsyncImage(
                                    model = uploadedImages[currentQuestion],
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                val aiIndex = currentQuestion - uploadedImages.size

                                Text(
                                    text = aiQuestions[aiIndex].question,
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text("🤖 AI Generated")
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                repeat(4) { index ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        onClick = {
                            selectedAnswers[currentQuestion] = index
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAnswers[currentQuestion] == index,
                                onClick = {
                                    selectedAnswers[currentQuestion] = index
                                }
                            )

                            Text(
                                text = "Option ${('A'.code + index).toChar()}",
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    if (currentQuestion > 0) {
                        currentQuestion--
                    }
                },
                enabled = currentQuestion > 0
            ) {
                Text("← Back")
            }

            Button(
                onClick = {
                    if (currentQuestion == totalQuestions - 1) {
                        showExitDialog = true
                    } else {
                        currentQuestion++
                    }
                }
            ) {
                Text(
                    if (currentQuestion == totalQuestions - 1)
                        "Finish"
                    else
                        "Next →"
                )
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                showExitDialog = false
            },
            title = {
                Text("End Quiz")
            },
            text = {
                Text("Are you sure you want to submit this quiz?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onBackClick()
                    }
                ) {
                    Text("End Quiz")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                    }
                ) {
                    Text("Continue Quiz")
                }
            }
        )
    }
}