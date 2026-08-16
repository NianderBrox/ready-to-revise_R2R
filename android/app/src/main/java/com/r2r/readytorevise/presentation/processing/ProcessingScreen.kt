package com.r2r.readytorevise.presentation.processing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.r2r.readytorevise.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun ProcessingScreen(
    navController: NavController
) {

    LaunchedEffect(Unit) {

        delay(3000)

        navController.navigate(Screen.Dashboard.route) {

            popUpTo(Screen.Processing.route) {
                inclusive = true
            }

        }

    }

    Column(

        modifier = Modifier.fillMaxSize(),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center

    ) {

        CircularProgressIndicator()

        Spacer(modifier = Modifier.height(32.dp))

        Text(

            text = "Processing your notes...",

            style = MaterialTheme.typography.headlineSmall

        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Analyzing uploaded files")

        Text("Generating questions")

        Text("Scheduling revision dates")

        Spacer(modifier = Modifier.height(24.dp))

        Text("Please wait...")

    }

}
