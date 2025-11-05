package dev.ferynnd.baguslaundry.ui

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Extension untuk Instant UTC
@RequiresApi(Build.VERSION_CODES.O)
fun Instant.toBranchTime(branchTimezone: String?, pattern: String = "dd MMM yyyy HH:mm:ss"): String {
    val zoneId = ZoneId.of(branchTimezone ?: "Asia/Jakarta")
    val zonedDateTime = this.atZone(zoneId)
    val formatter = DateTimeFormatter.ofPattern(pattern)
    return zonedDateTime.format(formatter)
}

@RequiresApi(Build.VERSION_CODES.O)
fun String.toBranchTime(branchTimezone: String?, pattern: String = "dd MMM yyyy HH:mm:ss"): String {
if (this.isEmpty()) return ""

    // Parsing string dari DB, misal "2025-11-02 16:33:57"
    val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val localDateTime = java.time.LocalDateTime.parse(this, inputFormatter)

    // Konversi ke timezone branch
    val zoneId = ZoneId.of(branchTimezone ?: "Asia/Jakarta")
    val zonedDateTime = localDateTime.atZone(ZoneId.of("UTC")).withZoneSameInstant(zoneId)

    // Format output
    val outputFormatter = DateTimeFormatter.ofPattern(pattern)
    val formattedTime = zonedDateTime.format(outputFormatter)

    // Tambahkan label zona waktu
    val zoneLabel = when (zoneId.id) {
        "Asia/Jakarta" -> "WIB"
        "Asia/Makassar" -> "WITA"
        "Asia/Jayapura" -> "WIT"
        else -> zoneId.id // fallback pakai ID timezone
    }

    return "$formattedTime $zoneLabel"
}

