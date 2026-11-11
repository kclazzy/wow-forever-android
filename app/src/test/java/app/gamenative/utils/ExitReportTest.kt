package app.gamenative.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExitReportTest {

    private val lowMemory = ExitRecord(
        timeMillis = 1_700_000_000_000L,
        processName = "app.wowforever",
        reason = "LOW_MEMORY",
        description = "lmkd kill",
        status = 0,
        pssKb = 1_900_000,
        rssKb = 2_400_000,
    )

    @Test
    fun noRecords_saysSoInsteadOfWritingAnEmptyFile() {
        assertTrue(formatExitReport(emptyList()).contains("no exit records"))
    }

    @Test
    fun record_showsReasonProcessAndMemoryInMegabytes() {
        val report = formatExitReport(listOf(lowMemory))
        assertTrue(report.contains("LOW_MEMORY"))
        assertTrue(report.contains("app.wowforever"))
        assertTrue(report.contains("description: lmkd kill"))
        assertTrue(report.contains("pss: 1855 MB"))
        assertTrue(report.contains("rss: 2343 MB"))
    }

    @Test
    fun newestRecordComesFirst() {
        val older = lowMemory.copy(timeMillis = 1_000L, reason = "CRASH")
        val newer = lowMemory.copy(timeMillis = 2_000L, reason = "CRASH_NATIVE")
        val report = formatExitReport(listOf(older, newer))
        assertTrue(report.indexOf("CRASH_NATIVE") < report.indexOf("reason: CRASH\n"))
    }

    @Test
    fun reasonNames_coverTheKillsThatMatterHere() {
        assertEquals("LOW_MEMORY", exitReasonName(3))
        assertEquals("CRASH_NATIVE", exitReasonName(5))
        assertEquals("SIGNALED", exitReasonName(2))
        assertEquals("UNKNOWN(999)", exitReasonName(999))
    }
}
