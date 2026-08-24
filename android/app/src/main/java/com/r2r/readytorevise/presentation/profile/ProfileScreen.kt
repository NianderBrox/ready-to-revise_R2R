package com.r2r.readytorevise.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.r2r.readytorevise.di.AppContainer
import com.r2r.readytorevise.ui.theme.Background
import com.r2r.readytorevise.ui.theme.OnSurface
import com.r2r.readytorevise.ui.theme.OnSurfaceVariant
import com.r2r.readytorevise.ui.theme.SkyBlue

@Composable
fun ProfileRoute(
    appContainer: AppContainer,
    navController: NavController,
) {
    val factory = viewModelFactory {
        initializer {
            ProfileViewModel(
                authRepository = appContainer.authRepository,
                dashboardRepository = appContainer.dashboardRepository,
            )
        }
    }

    val viewModel: ProfileViewModel = viewModel(factory = factory)

    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ProfileEffect.LoggedOut -> {
                    navController.navigate(com.r2r.readytorevise.navigation.Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    ProfileScreen(
        state = state,
        onEvent = viewModel::onEvent,
        navController = navController,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    navController: NavController,
) {
    Scaffold(
        containerColor = Background,
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
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SkyBlue
                )
            )
        }
    ) { padding ->
        when {
            state.loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape),
                        tint = SkyBlue
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Name",
                                fontSize = 14.sp,
                                color = OnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.name.ifBlank { "-" },
                                style = MaterialTheme.typography.titleLarge,
                                color = OnSurface
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Email",
                                fontSize = 14.sp,
                                color = OnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.email.ifBlank { "-" },
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurface
                            )
                        }
                    }

                    if (state.error != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(onClick = { onEvent(ProfileEvent.RetryLoad) }) {
                            Text("Retry")
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { onEvent(ProfileEvent.LogoutClicked) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Log Out")
                    }
                }
            }
        }
    }
}
