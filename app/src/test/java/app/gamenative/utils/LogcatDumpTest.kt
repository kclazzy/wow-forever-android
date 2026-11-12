package app.gamenative.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogcatDumpTest {

    @Test
    fun command_dumpsAndExitsInsteadOfFollowingTheLog() {
        assertTrue(LogcatDump.command().contains("-d"))
    }

    @Test
    fun command_limitsTheNumberOfLines() {
        val command = LogcatDump.command(maxLines = 500)
        assertEquals("500", command[command.indexOf("-t") + 1])
    }

    @Test
    fun command_readsTheCrashBufferToo() {
        val command = LogcatDump.command()
        assertTrue(command[command.indexOf("-b") + 1].contains("crash"))
    }
}
