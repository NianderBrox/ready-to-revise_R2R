package com.r2r.readytorevise.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.r2r.readytorevise.ui.theme.spacing
@Composable
fun R2RButton(

    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String = text
) {

    Button(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),

        enabled = enabled && !loading,

        onClick = onClick
    ) {

        val spacing = MaterialTheme.spacing

        if (loading) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {

                R2RLoading()

                Spacer(
                    modifier = Modifier.width(spacing.sm)
                )

                Text(
                    text = loadingText,
                    style = MaterialTheme.typography.titleMedium
                )
            }

        } else {

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}