package app.gamenative.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LogMirrorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun dir(name: String) = File(tempFolder.root, name).apply { mkdirs() }

    @Test
    fun copiesEachSourceFolderIntoItsOwnSubfolder() {
        val wine = dir("wine_logs").also { File(it, "wine_debug.log").writeText("wine line\n") }
        val crash = dir("crash_logs").also { File(it, "crash_1.txt").writeText("boom\n") }
        val dest = File(tempFolder.root, "app_logs")

        LogMirror.mirror(listOf(wine, crash), dest)

        assertEquals("wine line\n", File(dest, "wine_logs/wine_debug.log").readText())
        assertEquals("boom\n", File(dest, "crash_logs/crash_1.txt").readText())
    }

    @Test
    fun missingSourceIsIgnored() {
        val dest = File(tempFolder.root, "app_logs")
        LogMirror.mirror(listOf(File(tempFolder.root, "does_not_exist")), dest)
        assertFalse(File(dest, "does_not_exist").exists())
    }

    @Test
    fun hugeFileKeepsOnlyTheLastBytes() {
        val wine = dir("wine_logs").also { File(it, "wine_debug.log").writeText("0123456789") }
        val dest = File(tempFolder.root, "app_logs")

        LogMirror.mirror(listOf(wine), dest, maxBytes = 4)

        assertEquals("6789", File(dest, "wine_logs/wine_debug.log").readText())
    }

    @Test
    fun runningTwiceRefreshesTheCopy() {
        val wine = dir("wine_logs")
        val log = File(wine, "wine_debug.log").apply { writeText("first\n") }
        val dest = File(tempFolder.root, "app_logs")

        LogMirror.mirror(listOf(wine), dest)
        log.writeText("second run\n")
        LogMirror.mirror(listOf(wine), dest)

        assertEquals("second run\n", File(dest, "wine_logs/wine_debug.log").readText())
        assertTrue(File(dest, "wine_logs").listFiles()!!.size == 1)
    }
}
