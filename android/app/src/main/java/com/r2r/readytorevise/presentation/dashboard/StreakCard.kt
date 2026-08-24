package com.r2r.readytorevise.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.r2r.readytorevise.ui.theme.SkyBlue
import com.r2r.readytorevise.ui.theme.SkyBlueDark
import com.r2r.readytorevise.ui.theme.SkyBlueLight
import com.r2r.readytorevise.ui.theme.Streak
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun StreakCard(
    streakDays: Int = 0,
    activeDates: Set<LocalDate> = emptySet(),
) {
    val today = LocalDate.now()
    val monday = today.with(DayOfWeek.MONDAY)
    val week = (0..6).map { monday.plusDays(it.toLong()) }

    val streakDates = (0 until streakDays).map { today.minusDays(it.toLong()) }.toSet()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(SkyBlue, SkyBlueDark)
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Streak
                    )
                    Text(
                        text = " Streak",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { date ->
                        val active = date in streakDates || date in activeDates

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) Color.White
                                        else SkyBlueLight.copy(alpha = 0.6f)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
