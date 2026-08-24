package com.r2r.readytorevise.presentation.upload

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.r2r.readytorevise.ui.theme.Background
import com.r2r.readytorevise.ui.theme.OnSurface
import com.r2r.readytorevise.ui.theme.OnSurfaceVariant
import com.r2r.readytorevise.ui.theme.SkyBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    navController: NavController,
    images: List<Uri>,
    onAddImage: () -> Unit,
    onReplaceImage: (Int) -> Unit,
    onRemoveImage: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Preview Images",
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
                text = "Upload up to 5 images",
                color = OnSurface
            )

            Text(
                text = "${images.size}/5 Selected",
                color = OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            AiPrivacyNotice()

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(images) { index, uri ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 18.dp,
                                            topEnd = 18.dp
                                        )
                                    ),
                                contentScale = ContentScale.Crop
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        onReplaceImage(index)
                                    }
                                ) {
                                    Text("Replace", color = SkyBlue)
                                }

                                TextButton(
                                    onClick = {
                                        onRemoveImage(index)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.Red
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove", color = Color.Red)
                                }
                            }
                        }
                    }
                }

                if (images.size < 5) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clickable {
                                    onAddImage()
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
                                    text = "Add Image",
                                    color = SkyBlue
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (images.isEmpty()) {
                        Toast.makeText(context, "Please add at least one image", Toast.LENGTH_SHORT).show()
                    } else {
                        onSubmit()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
                enabled = images.isNotEmpty(),
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
