package app.gamenative.utils

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Saves this app's recent logcat lines. Native code on Android (the Mesa/Turnip Vulkan driver, bionic's fatal-signal
 * reports) logs there rather than to stderr, so a game process that dies at the native level leaves its last words
 * only in logcat. An app can read only its own lines, which is all that's wanted here.
 */
object LogcatDump {
    private const val MAX_LINES = 8000
    private const val TIMEOUT_SECONDS = 10L

    internal fun command(maxLines: Int = MAX_LINES): List<String> =
        listOf("logcat", "-d", "-v", "threadtime", "-b", "main,crash", "-t", maxLines.toString())

    fun write(dest: File) {
        dest.parentFile?.mkdirs()
        val process = ProcessBuilder(command()).redirectErrorStream(true).redirectOutput(dest).start()
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) process.destroyForcibly()
    }
}
