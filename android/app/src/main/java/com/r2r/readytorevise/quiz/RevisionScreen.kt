package com.r2r.readytorevise.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.r2r.readytorevise.di.AppContainer
import com.r2r.readytorevise.data.remote.NetworkConstants
import com.r2r.readytorevise.ReadyToReviseApplication
import com.r2r.readytorevise.presentation.quiz.QuizQuestion
import com.r2r.readytorevise.presentation.quiz.RevisionEffect
import com.r2r.readytorevise.presentation.quiz.RevisionEvent
import com.r2r.readytorevise.presentation.quiz.RevisionUiState
import com.r2r.readytorevise.presentation.quiz.RevisionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitCancellation
import kotlin.math.abs
import kotlin.math.roundToInt

private val ScreenBackground = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val AccentGreen = Color(0xFF22C55E)
private val AccentAmber = Color(0xFFF59E0B)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)

@Composable
fun RevisionRoute(
    appContainer: AppContainer,
    onBackClick: () -> Unit,
) {
    val factory = viewModelFactory {
        initializer {
            RevisionViewModel(
                studyItemsRepository = appContainer.studyItemsRepository,
                recommendationsRepository = appContainer.recommendationsRepository,
                reviewsRepository = appContainer.reviewsRepository,
            )
        }
    }

    val viewModel: RevisionViewModel = viewModel(factory = factory)

    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                RevisionEffect.NavigateBack -> onBackClick()
            }
        }
    }

    RevisionScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun RevisionScreen(
    state: RevisionUiState,
    onEvent: (RevisionEvent) -> Unit,
) {
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showPausedDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, state.finished, state.finishing) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !state.finished && !state.finishing) {
                showPausedDialog = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        awaitCancellation()
    }

    LaunchedEffect(state.finished, showPausedDialog) {
        while (!state.finished && !showPausedDialog) {
            delay(1000L)
            elapsedSeconds += 1
        }
    }

    BackHandler(enabled = !state.finished && !state.finishing && !showPausedDialog) {
        showPausedDialog = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            TopBar(
                elapsedSeconds = elapsedSeconds,
                onExit = {
                    if (state.finished || state.finishing) {
                        if (state.finished) onEvent(RevisionEvent.ExitConfirmed)
                    } else {
                        showExitDialog = true
                    }
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.finished) {
                FinishedContent(
                    state = state,
                    elapsedSeconds = elapsedSeconds,
                    onDone = { onEvent(RevisionEvent.ExitConfirmed) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                ActiveSessionContent(
                    state = state,
                    onEvent = onEvent,
                    onFinishClicked = { showFinishDialog = true },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finish session?", color = TextPrimary) },
            text = {
                Text(
                    "You answered ${state.selections.size} of ${state.totalQuestions} questions.",
                    color = TextMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    onEvent(RevisionEvent.FinishConfirmed)
                }) {
                    Text("Finish", color = AccentGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Keep going", color = TextMuted)
                }
            },
            containerColor = CardBackground,
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Go back?", color = TextPrimary) },
            text = {
                Text(
                    "Your revision progress will be lost. Are you sure you want to go back?",
                    color = TextMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onEvent(RevisionEvent.ExitConfirmed)
                }) {
                    Text("Go Back", color = AccentAmber)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Stay", color = TextMuted)
                }
            },
            containerColor = CardBackground,
        )
    }

    if (showPausedDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Quiz Paused", color = TextPrimary) },
            text = {
                Text(
                    "Your quiz has been paused. Would you like to resume or exit?",
                    color = TextMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPausedDialog = false
                }) {
                    Text("Resume", color = AccentGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPausedDialog = false
                    onEvent(RevisionEvent.ExitConfirmed)
                }) {
                    Text("Exit", color = AccentAmber)
                }
            },
            containerColor = CardBackground,
        )
    }
}

@Composable
private fun TopBar(
    elapsedSeconds: Long,
    onExit: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onExit) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "Review Session",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = formatTime(elapsedSeconds),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccentGreen,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF052E16))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ActiveSessionContent(
    state: RevisionUiState,
    onEvent: (RevisionEvent) -> Unit,
    onFinishClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (state.totalQuestions > 0) {
            LinearProgressIndicator(
                progress = {
                    (state.currentIndex + 1).toFloat() / state.totalQuestions.toFloat()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AccentGreen,
                trackColor = Color(0xFF334155),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Question ${state.currentIndex + 1} of ${state.totalQuestions}",
                fontSize = 13.sp,
                color = TextMuted,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        when {
            state.loading -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AccentGreen)
                }
            }

            state.error != null -> {
                ErrorContent(
                    message = state.error,
                    onRetry = { onEvent(RevisionEvent.RetryLoad) },
                    modifier = Modifier.weight(1f),
                )
            }

            else -> {
                val question = state.currentQuestion

                if (question == null) {
                    ErrorContent(
                        message = "No questions loaded.",
                        onRetry = { onEvent(RevisionEvent.RetryLoad) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    QuestionArea(
                        state = state,
                        question = question,
                        onEvent = onEvent,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NavigationButtons(
            state = state,
            onEvent = onEvent,
            onFinishClicked = onFinishClicked,
        )
    }
}

@Composable
private fun QuestionArea(
    state: RevisionUiState,
    question: QuizQuestion,
    onEvent: (RevisionEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val swipeThresholdPx = with(density) { 110.dp.toPx() }

    val marked = state.isMemorized(question)

    var dragX by remember(question.id) { mutableFloatStateOf(0f) }

    var crossedThreshold by remember(question.id) {
        mutableStateOf(false)
    }

    val progress = (-dragX / swipeThresholdPx).coerceIn(0f, 1f)

    val contentAlpha by animateFloatAsState(
        targetValue = if (marked) 0.55f else 1f,
        label = "memorizedDim",
    )

    Box(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = marked,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-8).dp)
                .zIndex(1f),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(AccentGreen),
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Memorized",
                    tint = Color(0xFF052E16),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(dragX.roundToInt(), 0) }
                .pointerInput(question.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (-dragX >= swipeThresholdPx) {
                                onEvent(RevisionEvent.MemorizeSwiped)
                            }
                            dragX = 0f
                            crossedThreshold = false
                        },
                        onDragCancel = {
                            dragX = 0f
                            crossedThreshold = false
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        val next = dragX + dragAmount

                        dragX = if (next < 0f) {
                            next.coerceAtLeast(-swipeThresholdPx * 1.5f)
                        } else {
                            next.coerceAtMost(swipeThresholdPx * 0.25f)
                        }

                        val nowCrossed = -dragX >= swipeThresholdPx

                        if (nowCrossed && !crossedThreshold) {
                            haptics.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                        }

                        crossedThreshold = nowCrossed
                    }
                },
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .alpha(contentAlpha)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        marked -> MemorizedChip()

                        progress > 0f -> SwipeStatusLabel(
                            text = if (marked) "RELEASE TO UNMARK"
                            else "RELEASE TO MARK MEMORIZED",
                            color = AccentGreen,
                        )

                        else -> SwipeStatusLabel(
                            text = "SWIPE LEFT IF YOU KNOW IT",
                            color = TextMuted,
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    ForgetDateChip(dateIso = question.expectedForgetDate)

                    ConfidenceChip(confidence = state.confidenceByItem[question.id])
                }

                Spacer(modifier = Modifier.height(16.dp))

                question.mediaDocumentId?.let { mediaId ->
                    Spacer(modifier = Modifier.height(12.dp))

                    DiagramImage(mediaDocumentId = mediaId)

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = question.question,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Start,
                )

                Spacer(modifier = Modifier.height(20.dp))

                question.options.forEachIndexed { index, optionText ->
                    OptionRow(
                        letter = ('A' + index),
                        text = optionText,
                        selected = state.selectedOptionFor(question) == index,
                        enabled = !marked && !state.finishing,
                        onClick = { onEvent(RevisionEvent.SelectOption(index)) },
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (!marked && progress > 0f) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = AccentGreen.copy(alpha = 0.35f * progress),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-4).dp)
                        .size(72.dp)
                        .graphicsLayer {
                            scaleX = 0.7f + 0.3f * progress
                            scaleY = 0.7f + 0.3f * progress
                        },
                )
            }
            }
        }
    }
}

@Composable
private fun DiagramImage(mediaDocumentId: String) {
    val context = LocalContext.current

    val app = context.applicationContext as ReadyToReviseApplication

    val url = NetworkConstants.BASE_URL +
        "api/v1/documents/" + mediaDocumentId + "/file"

    var imageState by remember(mediaDocumentId) {
        mutableStateOf<AsyncImagePainter.State?>(
            AsyncImagePainter.State.Empty,
        )
    }

    val loading = imageState == null ||
        imageState is AsyncImagePainter.State.Empty ||
        imageState is AsyncImagePainter.State.Loading

    val failed = imageState is AsyncImagePainter.State.Error

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B)),
    ) {
        if (loading) {
            SkeletonShimmer(modifier = Modifier.matchParentSize())
        }

        if (failed) {
            Text(
                text = "Diagram couldn't be loaded",
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build(),
            imageLoader = app.container.imageLoader,
            contentDescription = "Question diagram",
            contentScale = ContentScale.Fit,
            onState = { imageState = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .alpha(if (loading || failed) 0f else 1f),
        )
    }
}

@Composable
private fun SkeletonShimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")

    val pulse by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = pulse)),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color.White.copy(alpha = pulse * 0.8f)),
        )
    }
}

@Composable
private fun SwipeStatusLabel(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = color,
    )
}

@Composable
private fun MemorizedChip() {
    Text(
        text = "MEMORIZED",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = Color(0xFF052E16),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AccentGreen)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun ForgetDateChip(dateIso: String?) {
    if (dateIso == null) {
        return
    }

    val weekday = remember(dateIso) {
        runCatching {
            java.time.Instant.parse(dateIso)
                .atZone(java.time.ZoneId.systemDefault())
                .dayOfWeek
                .getDisplayName(
                    java.time.format.TextStyle.SHORT,
                    java.util.Locale.getDefault(),
                )
        }.getOrNull()
    } ?: return

    Text(
        text = "Forgotten by $weekday",
        fontSize = 11.sp,
        color = TextMuted,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun ConfidenceChip(confidence: Double?) {
    if (confidence == null || confidence <= 0.0) {
        return
    }

    Text(
        text = "${(confidence * 100).roundToInt()}%",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF052E16),
        modifier = Modifier
            .clip(CircleShape)
            .background(AccentGreen)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun OptionRow(
    letter: Char,
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFF14532D) else Color(0xFF334155))
            .border(
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) AccentGreen else Color(0xFF475569),
                ),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (selected) AccentGreen else Color(0xFF475569)),
        ) {
            Text(
                text = letter.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color(0xFF052E16) else TextPrimary,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            fontSize = 15.sp,
            color = TextPrimary,
        )
    }
}

@Composable
private fun NavigationButtons(
    state: RevisionUiState,
    onEvent: (RevisionEvent) -> Unit,
    onFinishClicked: () -> Unit,
) {
    val busy = state.finishing

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = { onEvent(RevisionEvent.BackClicked) },
            enabled = state.currentIndex > 0 && !busy,
            modifier = Modifier.weight(1f),
        ) {
            Text("Back", color = TextPrimary)
        }

        Button(
            onClick = {
                if (state.isLastQuestion) {
                    onFinishClicked()
                } else {
                    onEvent(RevisionEvent.NextClicked)
                }
            },
            enabled = !busy && state.questions.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = when {
                    busy -> "Saving…"
                    state.isLastQuestion -> "Finish"
                    else -> "Next"
                },
                color = Color(0xFF052E16),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                fontSize = 15.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
            ) {
                Text("Retry", color = Color(0xFF052E16), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FinishedContent(
    state: RevisionUiState,
    elapsedSeconds: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Session complete!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                ) {
                    SummaryRow("Reviewed", "${summary?.reviewedCount ?: 0}")

                    SummaryRow("Memorized", "${summary?.memorizedCount ?: 0}")

                    val failures = summary?.syncFailures ?: 0

                    if (failures > 0) {
                        SummaryRow("Sync failures", "$failures")
                    }

                    SummaryRow(
                        "Avg confidence",
                        summary?.averageConfidence
                            ?.let { "${(it * 100).roundToInt()}%" }
                            ?: "-",
                    )

                    SummaryRow("Time", formatTime(elapsedSeconds))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Done",
                    color = Color(0xFF052E16),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 15.sp, color = TextMuted)

        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
    }
}

private fun formatTime(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
