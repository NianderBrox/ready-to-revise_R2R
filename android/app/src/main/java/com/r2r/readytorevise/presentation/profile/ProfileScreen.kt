package com.r2r.readytorevise.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController
) {

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text("Profile")

                },

                navigationIcon = {

                    IconButton(

                        onClick = {

                            navController.popBackStack()

                        }

                    ) {

                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null
                        )

                    }

                }

            )

        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),

            horizontalAlignment = Alignment.CenterHorizontally

        ) {

            Spacer(modifier = Modifier.height(30.dp))

            Icon(

                imageVector = Icons.Default.AccountCircle,

                contentDescription = null,

                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape),

                tint = Color(0xFF23336F)

            )

            Spacer(modifier = Modifier.height(30.dp))

            Card(

                modifier = Modifier.fillMaxWidth()

            ) {

                Column(

                    modifier = Modifier.padding(20.dp)

                ) {

                    Text(

                        text = "Name",

                        fontSize = 14.sp,

                        color = Color.Gray

                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(

                        text = "Vaibhav Singh",

                        style = MaterialTheme.typography.titleLarge

                    )

                }

            }

        }

    }

}