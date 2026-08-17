package com.r2r.readytorevise.ui.components.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.r2r.readytorevise.ui.theme.SkyBlue

@Composable
fun DashboardButtons(
    onAddClick: () -> Unit,
    onRevisionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = onAddClick,
            modifier = Modifier
                .width(155.dp)
                .height(55.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SkyBlue
            )
        ) {
            Text(
                text = "＋ Add",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Button(
            onClick = onRevisionClick,
            modifier = Modifier
                .width(155.dp)
                .height(55.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SkyBlue
            )
        ) {
            Text(
                text = "📖 Revision",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
