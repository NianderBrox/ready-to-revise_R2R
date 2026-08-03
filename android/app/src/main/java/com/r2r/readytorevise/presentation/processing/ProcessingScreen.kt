package com.r2r.readytorevise.presentation.processing

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.r2r.readytorevise.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun ProcessingScreen(
    navController: NavController
) {

    var progress by remember {
        mutableFloatStateOf(0f)
    }

    var finished by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {

        for (i in 1..100) {
            delay(100)
            progress = i / 100f
        }

        finished = true
    }

    Scaffold(
        containerColor = Color(0xFF23336F)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Generating Quiz...",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(30.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${(progress * 100).toInt()} %",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    navController.navigate(Screen.Quiz.route)
                },
                enabled = finished,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    if (finished)
                        "Start Quiz"
                    else
                        "Generating..."
                )

            }

        }

    }

}