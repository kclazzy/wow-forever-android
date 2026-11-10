package app.gamenative.utils

import java.io.File
import java.io.RandomAccessFile

/**
 * Copies the app's private log folders (Android/data/<package>/files/...) to a folder the user can open with an
 * ordinary file manager. Each source folder gets its own subfolder, and a file bigger than [maxBytes] is cut to
 * its last [maxBytes] bytes, since the end of a log is where a crash shows up.
 */
object LogMirror {
    const val DEFAULT_MAX_BYTES = 5L * 1024 * 1024

    fun mirror(sources: List<File>, dest: File, maxBytes: Long = DEFAULT_MAX_BYTES) {
        for (source in sources) {
            val files = source.listFiles()?.filter { it.isFile } ?: continue
            val target = File(dest, source.name).apply { mkdirs() }
            for (file in files) {
                runCatching { copyTail(file, File(target, file.name), maxBytes) }
            }
        }
    }

    private fun copyTail(from: File, to: File, maxBytes: Long) {
        RandomAccessFile(from, "r").use { raf ->
            val skip = (raf.length() - maxBytes).coerceAtLeast(0L)
            raf.seek(skip)
            val bytes = ByteArray((raf.length() - skip).toInt())
            raf.readFully(bytes)
            to.writeBytes(bytes)
        }
    }
}
