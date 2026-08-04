package com.r2r.readytorevise.presentation.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.r2r.readytorevise.presentation.upload.UploadViewModel
import androidx.compose.ui.Alignment


@Composable
fun QuizScreen(
    uploadViewModel: UploadViewModel,
    onBackClick: () -> Unit
) {

    val uploadedImages = uploadViewModel.images


    var currentQuestion by remember {
        mutableIntStateOf(0)
    }


    var selectedOption by remember {
        mutableIntStateOf(-1)
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



    val totalQuestions =
        uploadedImages.size + aiQuestions.size



    Column(

        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)

    ) {



        Text(

            text = "Question ${currentQuestion + 1} / $totalQuestions",

            style = MaterialTheme.typography.titleMedium

        )



        Spacer(
            modifier = Modifier.height(12.dp)
        )



        LinearProgressIndicator(

            progress = {

                (currentQuestion + 1)
                    .toFloat()
                    .div(totalQuestions.toFloat())

            },

            modifier = Modifier.fillMaxWidth()

        )



        Spacer(
            modifier = Modifier.height(20.dp)
        )



        LazyColumn(

            modifier = Modifier
                .weight(1f)

        ) {


            item {



                Card(

                    modifier = Modifier.fillMaxWidth()

                ) {



                    Column(

                        modifier = Modifier.padding(15.dp)

                    ) {



                        if(currentQuestion < uploadedImages.size){



                            AsyncImage(

                                model = uploadedImages[currentQuestion],

                                contentDescription = null,

                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(350.dp),

                                contentScale = ContentScale.Fit

                            )



                        } else {



                            val aiIndex =
                                currentQuestion - uploadedImages.size



                            Text(

                                text = aiQuestions[aiIndex].question,

                                style = MaterialTheme.typography.titleLarge

                            )



                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )



                            AssistChip(

                                onClick = {},

                                label = {

                                    Text("🤖 AI Generated")

                                }

                            )

                        }


                    }


                }




                Spacer(
                    modifier = Modifier.height(20.dp)
                )




                repeat(4){ index ->



                    Card(

                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),

                        onClick = {

                            selectedOption = index

                        }

                    ){



                        Row(

                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(15.dp),

                            verticalAlignment = Alignment.CenterVertically

                        ){



                            RadioButton(

                                selected = selectedOption == index,

                                onClick = {

                                    selectedOption = index

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



        Spacer(
            modifier = Modifier.height(10.dp)
        )



        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.SpaceBetween

        ){



            Button(

                onClick = {


                    if(currentQuestion > 0){

                        currentQuestion--

                        selectedOption = -1

                    }


                },

                enabled = currentQuestion > 0

            ){

                Text("← Back")

            }





            Button(

                onClick = {


                    if(currentQuestion < totalQuestions - 1){

                        currentQuestion++

                        selectedOption = -1

                    }


                }

            ){


                Text(

                    if(currentQuestion == totalQuestions - 1)

                        "Finish"

                    else

                        "Next →"

                )


            }



        }



    }


}