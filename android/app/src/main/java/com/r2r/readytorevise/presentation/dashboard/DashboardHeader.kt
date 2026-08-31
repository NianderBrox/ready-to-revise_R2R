package com.r2r.readytorevise.ui.components.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DashboardHeader(
    userName: String,
    onProfileClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Hi, $userName 👋",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile",
                tint = Color.White,
                modifier = Modifier
                    .size(38.dp)
                    .clickable {
                        onProfileClick()
                    }
            )

            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.size(12.dp)
            )

            Box {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier
                        .size(34.dp)
                        .clickable {
                            expanded = true
                        }
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("Change Password")
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            expanded = false
                            onChangePasswordClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("Notification Settings")
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            expanded = false
                            onNotificationSettingsClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("Log Out")
                        },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Default.Logout,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            expanded = false
                            onLogoutClick()
                        }
                    )
                }
            }
        }
    }
}
