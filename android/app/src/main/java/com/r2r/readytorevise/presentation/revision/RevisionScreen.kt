package com.r2r.readytorevise.presentation.revision

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.r2r.readytorevise.navigation.Screen

data class RevisionQuiz(
    val topic: String,
    val questionCount: Int
)

@Composable
fun RevisionScreen(
    navController: NavController
) {

    val quizzes = listOf(

        RevisionQuiz(
            "Operating Systems",
            15
        ),

        RevisionQuiz(
            "Computer Networks",
            20
        ),

        RevisionQuiz(
            "Artificial Intelligence",
            12
        )

    )

    Column(

        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)

    ) {

        Text(

            text = "Today's Revision",

            style = MaterialTheme.typography.headlineSmall

        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(

            text = "Complete the quizzes scheduled for today.",

            style = MaterialTheme.typography.bodyMedium

        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(

            verticalArrangement = Arrangement.spacedBy(16.dp)

        ) {

            items(quizzes) { quiz ->

                Card(

                    modifier = Modifier.fillMaxWidth(),

                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),

                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 6.dp
                    )

                ) {

                    Column(

                        modifier = Modifier.padding(18.dp)

                    ) {

                        Text(

                            text = quiz.topic,

                            style = MaterialTheme.typography.titleLarge

                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            "📝 ${quiz.questionCount} Questions"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            "📅 Scheduled: Today"
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(

                            onClick = {

                                navController.navigate(Screen.Quiz.route)

                            },

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Text("Revise")

                        }

                    }

                }
            }

        }

    }

}