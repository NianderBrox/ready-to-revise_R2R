package com.r2r.readytorevise.presentation.upload

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.r2r.readytorevise.ui.theme.Background
import com.r2r.readytorevise.ui.theme.OnSurface
import com.r2r.readytorevise.ui.theme.OnSurfaceVariant
import com.r2r.readytorevise.ui.theme.SkyBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPreviewScreen(
    navController: NavController,
    uploadedFiles: List<UploadedFile>,
    onAddDocument: () -> Unit,
    onRemoveDocument: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Preview Documents",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SkyBlue
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Selected Documents",
                color = OnSurface,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "${uploadedFiles.size} file(s) selected",
                color = OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            AiPrivacyNotice()

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(uploadedFiles) { index, file ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (file.type == FileType.PDF) "\uD83D\uDCC4" else "\uD83D\uDCDD",
                                style = MaterialTheme.typography.headlineMedium
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.fileName,
                                    color = OnSurface,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = file.type.name,
                                    color = OnSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    onRemoveDocument(index)
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clickable {
                                onAddDocument()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = SkyBlue
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add Document",
                                color = SkyBlue
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (uploadedFiles.isEmpty()) {
                        Toast.makeText(context, "Please add at least one document", Toast.LENGTH_SHORT).show()
                    } else {
                        onSubmit()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
                enabled = uploadedFiles.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SkyBlue,
                    disabledContainerColor = SkyBlue.copy(alpha = 0.5f)
                )
            ) {
                Text("Submit")
            }
        }
    }
}


@Composable
fun AiPrivacyNotice() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2110)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFFF5B942)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Your documents are processed by AI to generate " +
                    "questions. Please don't upload personal or sensitive " +
                    "information.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFF5DFAE)
            )
        }
    }
}
