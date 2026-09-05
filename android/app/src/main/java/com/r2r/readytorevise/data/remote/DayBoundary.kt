package com.r2r.readytorevise.data.remote

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val isoFormatter = DateTimeFormatter.ISO_INSTANT

fun dueBeforeTodayISO(): String {
    val endOfToday = LocalDate.now()
        .plusDays(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
    return isoFormatter.format(endOfToday)
}
