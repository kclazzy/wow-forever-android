package app.gamenative.utils

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExeProbeTest {

    /** A minimal PE: one ".text" section at RVA 0x1000, stored at file offset 0x400, 0x200 bytes long. */
    private fun fakePe(): ByteArray {
        val buf = ByteBuffer.allocate(0x600).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(0, 'M'.code.toByte()).put(1, 'Z'.code.toByte())
        val pe = 0x80
        buf.putInt(0x3C, pe)
        buf.putInt(pe, 0x00004550)
        buf.putShort(pe + 6, 1) // sections
        buf.putShort(pe + 20, 0xF0.toShort()) // optional header size
        val table = pe + 24 + 0xF0
        ".text".toByteArray().forEachIndexed { i, b -> buf.put(table + i, b) }
        buf.putInt(table + 8, 0x200) // virtual size
        buf.putInt(table + 12, 0x1000) // rva
        buf.putInt(table + 16, 0x200) // raw size
        buf.putInt(table + 20, 0x400) // raw offset
        buf.putInt(0x400 + 0x10, 0xD503201F.toInt()) // nop at rva 0x1010
        return buf.array()
    }

    @Test
    fun parsesTheSectionTable() {
        val section = parsePeSections(fakePe()).single()
        assertEquals(PeSection(".text", 0x1000, 0x200, 0x400, 0x200), section)
    }

    @Test
    fun notAPeFile_givesNoSections() {
        assertTrue(parsePeSections(ByteArray(0x100)).isEmpty())
    }

    @Test
    fun rvaMapsToFileOffsetInsideItsSection() {
        val sections = parsePeSections(fakePe())
        assertEquals(0x410L, rvaToFileOffset(sections, 0x1010)?.second)
        assertNull(rvaToFileOffset(sections, 0x5000))
    }

    @Test
    fun write_dumpsTheInstructionWordAtTheRva() {
        val dir = File(System.getProperty("java.io.tmpdir"), "exeprobe-${System.nanoTime()}").apply { mkdirs() }
        try {
            val exe = File(dir, "game.exe").apply { writeBytes(fakePe()) }
            val out = File(dir, "out/exe_probe.txt")
            ExeProbe.write(exe, listOf(0x1010L, 0x9000L), out)
            val text = out.readText()
            assertTrue(text.contains("d503201f"))
            assertTrue(text.contains("rva 0x9000: not in file"))
        } finally {
            dir.deleteRecursively()
        }
    }
}
