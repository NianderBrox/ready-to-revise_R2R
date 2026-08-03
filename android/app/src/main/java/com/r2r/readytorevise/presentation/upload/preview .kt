    package com.r2r.readytorevise.presentation.upload

    import android.net.Uri
    import androidx.compose.foundation.Image
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.itemsIndexed
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Add
    import androidx.compose.material.icons.filled.ArrowBack
    import androidx.compose.material3.*
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.layout.ContentScale
    import androidx.compose.ui.unit.dp
    import androidx.navigation.NavController
    import coil3.compose.rememberAsyncImagePainter

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PreviewScreen(

        navController: NavController,

        images: List<Uri>,

        onAddImage: () -> Unit,

        onReplaceImage: (Int) -> Unit,

        onSubmit: () -> Unit

    ) {

        Scaffold(

            containerColor = Color(0xFF23336F),

            topBar = {

                TopAppBar(

                    title = {

                        Text(
                            "Preview Images",
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
                                Icons.Default.ArrowBack,
                                null,
                                tint = Color.White
                            )

                        }

                    },

                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF23336F)
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

                    color = Color.White

                )

                Text(

                    text = "${images.size}/5 Selected",

                    color = Color.LightGray

                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(

                    modifier = Modifier.weight(1f),

                    verticalArrangement = Arrangement.spacedBy(16.dp)

                ) {

                    itemsIndexed(images) { index, uri ->

                        Card(

                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2C377C)
                            ),

                            shape = RoundedCornerShape(18.dp)

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

                                TextButton(

                                    onClick = {

                                        onReplaceImage(index)

                                    },

                                    modifier = Modifier.align(Alignment.End)

                                ) {

                                    Text("Replace")

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
                                    containerColor = Color(0xFF2C377C)
                                ),

                                shape = RoundedCornerShape(18.dp)

                            ) {

                                Column(

                                    modifier = Modifier.fillMaxSize(),

                                    horizontalAlignment = Alignment.CenterHorizontally,

                                    verticalArrangement = Arrangement.Center

                                ) {

                                    Icon(

                                        Icons.Default.Add,

                                        null,

                                        tint = Color.White

                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(

                                        "Add Image",

                                        color = Color.White

                                    )

                                }

                            }

                        }

                    }

                }

                Button(

                    onClick = onSubmit,

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF43D17A)
                    )

                ) {

                    Text("Submit")

                }

            }

        }

    }