package com.r2r.readytorevise.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.r2r.readytorevise.navigation.Screen
import com.r2r.readytorevise.ui.components.dashboard.DashboardButtons
import com.r2r.readytorevise.ui.components.dashboard.DashboardHeader
import com.r2r.readytorevise.ui.components.dashboard.StreakCard
import com.r2r.readytorevise.ui.components.dashboard.TopicsCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController
) {

    Scaffold(

        containerColor = Color(0xFF23336F),

        topBar = {

            TopAppBar(

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF23336F)
                ),

                title = {

                    DashboardHeader(
                        userName = "Vaibhav Singh",
                        onProfileClick = {
                            navController.navigate(Screen.Profile.route)
                        }
                    )

                }

            )

        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF23336F))
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),

            verticalArrangement = Arrangement.spacedBy(20.dp)

        ) {

            Spacer(modifier = Modifier.height(8.dp))

            StreakCard()

            TopicsCard()

            DashboardButtons(

                onAddClick = {

                    navController.navigate(Screen.Upload.route)

                },

                onRevisionClick = {

                    navController.navigate(Screen.Revision.route)

                }

            )

            Spacer(modifier = Modifier.height(30.dp))

        }

    }

}