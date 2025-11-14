package dev.ferynnd.baguslaundry.ui

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.LocalDateTime
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

    return try {
        // 1. Parsing input "2025-11-14 20:10:32" sebagai LOCAL TIME cabang
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val localDateTime = LocalDateTime.parse(this, inputFormatter)

        // 2. Tentukan timezone cabang asal
        val branchZone = ZoneId.of(branchTimezone ?: "Asia/Jakarta")

        // 3. Local time → zoned date time (LOCAL BRANCH TIME)
        val branchZoned = localDateTime.atZone(branchZone)

        // 4. Konversi ke UTC
        val utcZoned = branchZoned.withZoneSameInstant(ZoneId.of("UTC"))

        // 5. Konversi UTC → branch timezone lagi (sesuai kebutuhan)
        val finalBranchZoned = utcZoned.withZoneSameInstant(branchZone)

        // 6. Format output
        val outputFormatter = DateTimeFormatter.ofPattern(pattern)
        val formattedTime = finalBranchZoned.format(outputFormatter)

        // 7. Label WIB/WITA/WIT
        val zoneLabel = when (branchZone.id) {
            "Asia/Jakarta" -> "WIB"
            "Asia/Makassar" -> "WITA"
            "Asia/Jayapura" -> "WIT"
            else -> branchZone.id
        }

        "$formattedTime $zoneLabel"
    } catch (e: Exception) {
        "-"
    }
}

