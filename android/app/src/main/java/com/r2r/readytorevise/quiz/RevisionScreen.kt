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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.r2r.readytorevise.presentation.upload.UploadViewModel
import com.r2r.readytorevise.ui.theme.Success
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

data class Question(
    val id: String,
    val question: String,
    val options: List<String> = emptyList(),
    val correctAnswer: Int,
    val aiGenerated: Boolean = false
)

@Composable
fun RevisionScreen(
    uploadViewModel: UploadViewModel,
    onBackClick: () -> Unit
) {
    val uploadedImages = uploadViewModel.images
    var elapsedSeconds by remember {
        mutableIntStateOf(0)
    }

    var showCongrats by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(showCongrats) {
        if (!showCongrats) {
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    var showExitDialog by remember {
        mutableStateOf(false)
    }

    var showFinishDialog by remember {
        mutableStateOf(false)
    }

    BackHandler {
        if (showCongrats) {
            onBackClick()
        } else {
            showExitDialog = true
        }
    }

    var currentQuestion by remember {
        mutableIntStateOf(0)
    }
    val offsetX = remember {
        Animatable(0f)
    }

    val scope = rememberCoroutineScope()

    val density = LocalDensity.current

    val memorizedQuestions = remember {
        mutableStateSetOf<Int>()
    }
    val isCurrentMemorized by remember {
        derivedStateOf { memorizedQuestions.contains(currentQuestion) }
    }
    val currentMemorized by rememberUpdatedState(isCurrentMemorized)

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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationX = offsetX.value
                                alpha = if (currentMemorized) 0.5f else 1f
                            }
                            .pointerInput(currentQuestion) {
                                val swipeThresholdPx = with(density) { -180.dp.toPx() }
                                val swipeDistancePx = with(density) { -200.dp.toPx() }

                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        scope.launch {
                                            if (offsetX.value <= swipeThresholdPx) {
                                                if (currentMemorized) {
                                                    memorizedQuestions.remove(currentQuestion)
                                                } else {
                                                    memorizedQuestions.add(currentQuestion)
                                                }

                                                offsetX.animateTo(
                                                    targetValue = swipeDistancePx,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    )
                                                )
                                                offsetX.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    )
                                                )
                                            } else {
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
                                        val resistance = 0.5f
                                        val newOffset = (offsetX.value + dragAmount * resistance)
                                            .coerceIn(swipeDistancePx, 0f)

                                        scope.launch {
                                            offsetX.snapTo(newOffset)
                                        }
                                    }
                                )
                            }
                    ) {
                        Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
                            Column(
                                modifier = Modifier.padding(15.dp)
                            ) {
                                if (currentQuestion < uploadedImages.size) {
                                    AsyncImage(
                                        model = uploadedImages[currentQuestion],
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp),
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

                                Spacer(modifier = Modifier.height(16.dp))

                                repeat(4) { index ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp),
                                        onClick = {
                                            if (!currentMemorized) {
                                                selectedAnswers[currentQuestion] = index
                                            }
                                        },
                                        enabled = !currentMemorized
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
                                                    if (!currentMemorized) {
                                                        selectedAnswers[currentQuestion] = index
                                                    }
                                                },
                                                enabled = !currentMemorized
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
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .graphicsLayer {
                                val swipeThreshold = with(density) { 150.dp.toPx() }
                                val maxSlide = with(density) { 200.dp.toPx() }
                                translationX = if (currentMemorized) {
                                    0f
                                } else {
                                    val progress = (-offsetX.value / swipeThreshold).coerceIn(0f, 1f)
                                    maxSlide * (1f - progress)
                                }
                            }
                            .background(Success, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "✓ Memorized",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
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
                                showFinishDialog = true
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
        }
    }

    if (showCongrats) {
        val memorizedCount = memorizedQuestions.size
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        val checkScale = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            checkScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .graphicsLayer {
                            scaleX = checkScale.value
                            scaleY = checkScale.value
                        },
                    tint = Success
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Revision Complete!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Great job keeping up with your studies!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Your Session",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$totalQuestions",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Reviewed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$memorizedCount",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Success
                                )
                                Text(
                                    text = "Memorized",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%02d:%02d", minutes, seconds),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Time",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = onBackClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Back to Dashboard",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                showExitDialog = false
            },
            title = {
                Text("Go back?")
            },
            text = {
                Text("Your revision progress will be lost. Are you sure you want to go back?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onBackClick()
                    }
                ) {
                    Text("Go Back")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                    }
                ) {
                    Text("Stay")
                }
            }
        )
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = {
                showFinishDialog = false
            },
            title = {
                Text("Finish Revision")
            },
            text = {
                Text("Are you sure you want to finish this revision?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFinishDialog = false
                        showCongrats = true
                    }
                ) {
                    Text("Finish")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showFinishDialog = false
                    }
                ) {
                    Text("Continue")
                }
            }
        )
    }
}
