package com.r2r.readytorevise.presentation.upload

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPreviewScreen(

    navController: NavController,

    uploadedFiles: List<UploadedFile>,

    onAddDocument: () -> Unit,

    onRemoveDocument: (Int) -> Unit,

    onSubmit: () -> Unit

) {

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text("Preview Documents")

                }

            )

        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)

        ) {

            Text(

                text = "Selected Documents",

                style = MaterialTheme.typography.titleLarge

            )

            Spacer(modifier = Modifier.height(16.dp))

            uploadedFiles.forEachIndexed { index, file ->

                Card(

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)

                ) {

                    Row(

                        modifier = Modifier.padding(16.dp)

                    ) {

                        Text(

                            text = if (file.type == FileType.PDF)
                                "📄"
                            else
                                "📝",

                            style = MaterialTheme.typography.headlineMedium

                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {

                            Text(

                                text = file.fileName,

                                style = MaterialTheme.typography.titleMedium

                            )

                            Text(

                                text = file.type.name

                            )

                        }

                        Spacer(modifier = Modifier.weight(1f))

                        TextButton(

                            onClick = {

                                onRemoveDocument(index)

                            }

                        ) {

                            Text("Remove")

                        }

                    }

                }

            }

        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(

            modifier = Modifier
                .fillMaxWidth()
                .clickable {

                    onAddDocument()

                }

        ) {

            Column(

                modifier = Modifier.padding(20.dp)

            ) {

                Text(

                    text = "➕ Add Document",

                    style = MaterialTheme.typography.titleMedium

                )

            }

        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(

            onClick = {

                onSubmit()

            },

            modifier = Modifier.fillMaxWidth()

        ) {

            Text("Submit")

        }

    }

}

