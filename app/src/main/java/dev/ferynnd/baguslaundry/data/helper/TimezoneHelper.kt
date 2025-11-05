package dev.ferynnd.baguslaundry.data.helper

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object TimezoneHelper {

    /**
     * Konversi waktu UTC dari server ke timezone branch tertentu
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun convertUtcToBranchTimezone(
        utcTimeString: String?,
        branchTimezone: String?,
        outputFormat: String = "dd MMM yyyy, HH:mm"
    ): String {
        if (utcTimeString.isNullOrBlank() || branchTimezone.isNullOrBlank()) {
            return "-"
        }

        return try {
            // Parse UTC time dari server (format ISO)
            val instant = Instant.parse(utcTimeString)

            // Konversi ke timezone branch
            val zoneId = ZoneId.of(branchTimezone)
            val zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId)

            // Tentukan singkatan zona waktu (khusus Indonesia)
            val timeAbbreviation = getTimezoneAbbreviation(branchTimezone)

            // Format output waktu
            val formatter = DateTimeFormatter.ofPattern(outputFormat, Locale("id", "ID"))
            "${zonedDateTime.format(formatter)} $timeAbbreviation"
        } catch (e: Exception) {
            // Fallback jika parsing gagal
            tryParseAlternativeFormat(utcTimeString, branchTimezone, outputFormat)
        }
    }

    /**
     * Tentukan singkatan zona waktu berdasarkan Zone ID
     */
    private fun getTimezoneAbbreviation(branchTimezone: String): String {
        return when (branchTimezone) {
            "Asia/Jakarta" -> "WIB"
            "Asia/Makassar" -> "WITA"
            "Asia/Jayapura" -> "WIT"
            else -> branchTimezone.substringAfterLast('/') // fallback
        }
    }

    /**
     * Fallback parser untuk format waktu alternatif (yyyy-MM-dd HH:mm:ss)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun tryParseAlternativeFormat(
        timeString: String,
        branchTimezone: String,
        outputFormat: String
    ): String {
        return try {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val localDateTime = java.time.LocalDateTime.parse(timeString, inputFormatter)

            val instant = localDateTime.atZone(ZoneId.of("UTC")).toInstant()
            val zoneId = ZoneId.of(branchTimezone)
            val zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId)

            val formatter = DateTimeFormatter.ofPattern(outputFormat, Locale("id", "ID"))
            val timeAbbreviation = getTimezoneAbbreviation(branchTimezone)

            "${zonedDateTime.format(formatter)} $timeAbbreviation"
        } catch (e: Exception) {
            timeString
        }
    }

    /**
     * Konversi waktu UTC ke timezone branch (untuk Android versi lama < API 26)
     */
    fun convertUtcToBranchTimezoneLegacy(
        utcTimeString: String?,
        branchTimezone: String?,
        outputFormat: String = "dd MMM yyyy, HH:mm"
    ): String {
        if (utcTimeString.isNullOrBlank() || branchTimezone.isNullOrBlank()) {
            return "-"
        }

        return try {
            val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            utcFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = utcFormat.parse(utcTimeString) ?: return utcTimeString

            val branchFormat = SimpleDateFormat(outputFormat, Locale("id", "ID"))
            branchFormat.timeZone = TimeZone.getTimeZone(branchTimezone)

            val timeAbbreviation = getTimezoneAbbreviation(branchTimezone)
            "${branchFormat.format(date)} $timeAbbreviation"
        } catch (e: Exception) {
            tryParseAlternativeFormatLegacy(utcTimeString, branchTimezone, outputFormat)
        }
    }

    /**
     * Fallback parser untuk Android versi lama
     */
    private fun tryParseAlternativeFormatLegacy(
        timeString: String,
        branchTimezone: String,
        outputFormat: String
    ): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(timeString) ?: return timeString

            val branchFormat = SimpleDateFormat(outputFormat, Locale("id", "ID"))
            branchFormat.timeZone = TimeZone.getTimeZone(branchTimezone)

            val timeAbbreviation = getTimezoneAbbreviation(branchTimezone)
            "${branchFormat.format(date)} $timeAbbreviation"
        } catch (e: Exception) {
            timeString
        }
    }

    /**
     * Get formatted date untuk bulan/tahun (untuk filtering)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getMonthYearFromUtc(
        utcTimeString: String?,
        branchTimezone: String?
    ): String {
        return convertUtcToBranchTimezone(utcTimeString, branchTimezone, "yyyy-MM")
    }
}
