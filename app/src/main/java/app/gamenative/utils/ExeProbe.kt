package app.gamenative.utils

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** One section of a PE image: where it sits in memory (RVA) and in the file. */
data class PeSection(val name: String, val virtualAddress: Long, val virtualSize: Long, val rawOffset: Long, val rawSize: Long)

/** Parses the section table from the first bytes of a PE file; empty if it isn't one. */
internal fun parsePeSections(header: ByteArray): List<PeSection> {
    if (header.size < 0x40 || header[0] != 'M'.code.toByte() || header[1] != 'Z'.code.toByte()) return emptyList()
    val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
    val pe = buf.getInt(0x3C)
    if (pe < 0 || pe + 24 > header.size || buf.getInt(pe) != 0x00004550) return emptyList()
    val count = buf.getShort(pe + 6).toInt() and 0xFFFF
    val table = pe + 24 + (buf.getShort(pe + 20).toInt() and 0xFFFF)
    return (0 until count).mapNotNull { i ->
        val at = table + i * 40
        if (at + 40 > header.size) return@mapNotNull null
        val name = String(header, at, 8, Charsets.US_ASCII).trimEnd('\u0000')
        fun u32(offset: Int) = buf.getInt(at + offset).toLong() and 0xFFFFFFFFL
        PeSection(name, virtualAddress = u32(12), virtualSize = u32(8), rawOffset = u32(20), rawSize = u32(16))
    }
}

/** File offset of [rva], or null when it lies outside every section's on-disk data. */
internal fun rvaToFileOffset(sections: List<PeSection>, rva: Long): Pair<PeSection, Long>? =
    sections.firstOrNull { rva >= it.virtualAddress && rva < it.virtualAddress + it.rawSize }
        ?.let { it to (it.rawOffset + (rva - it.virtualAddress)) }

object ExeProbe {
    private const val HEADER_BYTES = 8192
    private const val WORDS_BEFORE = 2
    private const val WORDS = 6

    /**
     * Writes the ARM64 instruction words found at [rvas] in [exe] to [dest], so the instructions that raised
     * "illegal instruction" exceptions can be decoded and compared with what the device's CPU supports.
     */
    fun write(exe: File, rvas: List<Long>, dest: File) {
        if (!exe.isFile) return
        RandomAccessFile(exe, "r").use { raf ->
            val header = ByteArray(minOf(HEADER_BYTES.toLong(), raf.length()).toInt()).also { raf.readFully(it) }
            val sections = parsePeSections(header)
            val text = buildString {
                appendLine("file: ${exe.name} size=${raf.length()}")
                sections.forEach { appendLine("section ${it.name} rva=0x${it.virtualAddress.toString(16)} vsize=0x${it.virtualSize.toString(16)} raw=0x${it.rawOffset.toString(16)} rawsize=0x${it.rawSize.toString(16)}") }
                for (rva in rvas) {
                    val hit = rvaToFileOffset(sections, rva)
                    if (hit == null) { appendLine("rva 0x${rva.toString(16)}: not in file"); continue }
                    val start = maxOf(0L, hit.second - WORDS_BEFORE * 4)
                    val bytes = ByteArray(minOf(WORDS * 4L, raf.length() - start).toInt())
                    raf.seek(start); raf.readFully(bytes)
                    val words = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                    val hex = (0 until bytes.size / 4).joinToString(" ") { "%08x".format(words.getInt(it * 4)) }
                    appendLine("rva 0x${rva.toString(16)} (${hit.first.name}, words from rva 0x${(rva - (hit.second - start)).toString(16)}): $hex")
                }
            }
            dest.parentFile?.mkdirs()
            dest.writeText(text)
        }
    }
}
