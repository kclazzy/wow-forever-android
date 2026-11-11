package app.gamenative.utils

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One entry of Android's record of why a process of this app ended (ApplicationExitInfo). */
data class ExitRecord(
    val timeMillis: Long,
    val processName: String,
    val reason: String,
    val description: String,
    val status: Int,
    val pssKb: Long,
    val rssKb: Long,
)

private val EXIT_REASON_NAMES = mapOf(
    0 to "UNKNOWN", 1 to "EXIT_SELF", 2 to "SIGNALED", 3 to "LOW_MEMORY", 4 to "CRASH", 5 to "CRASH_NATIVE",
    6 to "ANR", 7 to "INITIALIZATION_FAILURE", 8 to "PERMISSION_CHANGE", 9 to "EXCESSIVE_RESOURCE_USAGE",
    10 to "USER_REQUESTED", 11 to "USER_STOPPED", 12 to "DEPENDENCY_DIED", 13 to "OTHER",
    14 to "FREEZER", 15 to "PACKAGE_STATE_CHANGE", 16 to "PACKAGE_UPDATED",
)

internal fun exitReasonName(code: Int): String = EXIT_REASON_NAMES[code] ?: "UNKNOWN($code)"

/** Human-readable report, newest record first. */
internal fun formatExitReport(records: List<ExitRecord>): String {
    if (records.isEmpty()) return "no exit records available\n"
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    return records.sortedByDescending { it.timeMillis }.joinToString("\n") { r ->
        buildString {
            appendLine("time: ${fmt.format(Date(r.timeMillis))}")
            appendLine("process: ${r.processName}")
            appendLine("reason: ${r.reason}")
            appendLine("status: ${r.status}")
            appendLine("description: ${r.description}")
            appendLine("pss: ${r.pssKb / 1024} MB")
            appendLine("rss: ${r.rssKb / 1024} MB")
        }
    }
}

object ExitReport {
    private const val MAX_RECORDS = 10
    private const val MAX_TRACE_BYTES = 1_000_000

    /**
     * Writes "exit_reasons.txt" (and the newest native-crash tombstone, if any) into [destDir]. Android keeps these
     * records itself, so they survive a kill that left no log of our own (low memory, native crash, signal).
     * Needs API 30+; does nothing below that.
     */
    fun write(context: Context, destDir: File) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val infos = am.getHistoricalProcessExitReasons(context.packageName, 0, MAX_RECORDS)
        destDir.mkdirs()
        val records = infos.map {
            ExitRecord(it.timestamp, it.processName, exitReasonName(it.reason), it.description ?: "", it.status, it.pss, it.rss)
        }
        File(destDir, "exit_reasons.txt").writeText(formatExitReport(records))

        infos.firstOrNull { it.reason == ApplicationExitInfo.REASON_CRASH_NATIVE }?.let { info ->
            runCatching {
                info.traceInputStream?.use { stream ->
                    File(destDir, "native_crash_${info.timestamp}.pb").writeBytes(stream.readNBytes(MAX_TRACE_BYTES))
                }
            }
        }
    }
}
