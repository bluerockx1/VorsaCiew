package com.vorsaciew.app.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Haversine great-circle distance in km between two lat/lng pairs. */
fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return R * 2 * atan2(sqrt(a), sqrt(1 - a))
}

/** Haversine distance in miles. */
fun distanceMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double =
    distanceKm(lat1, lon1, lat2, lon2) * 0.621371

fun Long.toFormattedDate(pattern: String = "MMM d, yyyy"): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))

fun Long.toFormattedDateTime(pattern: String = "MMM d 'at' h:mm a"): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < 60_000L              -> "just now"
        diff < 3_600_000L           -> "${diff / 60_000}m ago"
        diff < 86_400_000L          -> "${diff / 3_600_000}h ago"
        diff < 7 * 86_400_000L      -> "${diff / 86_400_000}d ago"
        else                        -> toFormattedDate()
    }
}
