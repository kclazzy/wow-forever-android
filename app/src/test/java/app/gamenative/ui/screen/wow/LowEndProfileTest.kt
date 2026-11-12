package app.gamenative.ui.screen.wow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LowEndProfileTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // --- selectPerformanceCores -------------------------------------------------------------------------

    @Test
    fun snapdragon685_picksTheFourA73Cores() {
        val freqs = listOf(1_900_000L, 1_900_000, 1_900_000, 1_900_000, 2_800_000, 2_800_000, 2_800_000, 2_800_000)
        assertEquals("4,5,6,7", selectPerformanceCores(freqs))
    }

    @Test
    fun bigCoresFirst_picksTheLowIndexes() {
        val freqs = listOf(2_800_000L, 2_800_000, 2_800_000, 2_800_000, 1_900_000, 1_900_000, 1_900_000, 1_900_000)
        assertEquals("0,1,2,3", selectPerformanceCores(freqs))
    }

    @Test
    fun threeClusterChip_keepsPrimeAndMiddleButNotLittle() {
        val freqs = listOf(1_800_000L, 1_800_000, 1_800_000, 1_800_000, 2_500_000, 2_500_000, 2_500_000, 3_000_000)
        assertEquals("4,5,6,7", selectPerformanceCores(freqs))
    }

    @Test
    fun identicalCores_returnsNullSoTheContainerKeepsItsDefault() {
        assertNull(selectPerformanceCores(List(8) { 2_400_000L }))
    }

    @Test
    fun unreadableFrequencies_returnsNull() {
        assertNull(selectPerformanceCores(List(8) { 0L }))
        assertNull(selectPerformanceCores(emptyList()))
    }

    // --- isLowEndAdreno6xx ------------------------------------------------------------------------------

    @Test
    fun adreno610_isLowEnd() = assertTrue(isLowEndAdreno6xx("Adreno (TM) 610", ""))

    @Test
    fun adreno619_isLowEnd() = assertTrue(isLowEndAdreno6xx("Adreno (TM) 619", ""))

    @Test
    fun adreno650_isNotLowEnd() = assertFalse(isLowEndAdreno6xx("Adreno (TM) 650", ""))

    @Test
    fun adreno740_isNotLowEnd() = assertFalse(isLowEndAdreno6xx("Adreno (TM) 740", "SM8550"))

    @Test
    fun adreno830_isNotLowEnd() = assertFalse(isLowEndAdreno6xx("Adreno (TM) 830", "SM8750"))

    @Test
    fun unknownRenderer_fallsBackToSnapdragon685SocModel() = assertTrue(isLowEndAdreno6xx("", "SM6225"))

    @Test
    fun maliDevice_isNotLowEnd() = assertFalse(isLowEndAdreno6xx("Mali-G57 MC2", "MT6789"))

    // --- Config.wtf on first boot -----------------------------------------------------------------------

    private fun configWtf(root: File) = File(root, "${WowClientDownloader.FLAVOR_DIR}/WTF/Config.wtf")

    private fun install(): File {
        val root = tempFolder.newFolder()
        File(root, WowClientDownloader.BUILD_INFO).writeText(
            "Branch!STRING:0|Product!STRING:0\nus|${WowClientDownloader.TARGET_PRODUCT}\n",
        )
        return root
    }

    private fun lines(root: File, key: String) = configWtf(root).readLines().filter { it.startsWith("SET $key ") }

    @Test
    fun lowEndFirstBoot_usesShortDrawDistanceAndFrameCap() {
        val root = install()
        ensureGameConfig(root, "960x540", null, lowEnd = true)
        assertEquals(listOf("SET farclip \"600\""), lines(root, "farclip"))
        assertEquals(listOf("SET maxFPS \"30\""), lines(root, "maxFPS"))
    }

    @Test
    fun defaultFirstBoot_isUnchanged() {
        val root = install()
        ensureGameConfig(root, "1920x1080", null)
        assertEquals(listOf("SET farclip \"1200\""), lines(root, "farclip"))
        assertTrue(lines(root, "maxFPS").isEmpty())
    }

    @Test
    fun lowEnd_doesNotOverwriteTheExistingConfig() {
        val root = install()
        configWtf(root).apply { parentFile?.mkdirs() }.writeText("SET farclip \"1200\"\n")
        ensureGameConfig(root, "960x540", null, lowEnd = true)
        assertEquals(listOf("SET farclip \"1200\""), lines(root, "farclip"))
        assertTrue(lines(root, "maxFPS").isEmpty())
    }

    @Test
    fun dxwrapperConfig_lowEnd_capsCompilerThreadsAndVideoMemory() {
        val config = dxwrapperConfig("2.4.1-1", lowEnd = true)
        assertTrue(config.startsWith("version=2.4.1-1"))
        assertTrue(config.contains("numCompilerThreads=2"))
        assertTrue(config.contains("maxDeviceMemory=2048"))
    }

    @Test
    fun dxwrapperConfig_otherDevices_isUnchanged() {
        assertEquals("version=2.4.1-1", dxwrapperConfig("2.4.1-1", lowEnd = false))
    }

    @Test
    fun lowEndScreenSize_isOneOfTheSelectableResolutions() {
        assertTrue(RenderResolution.entries.any { it.size == LOW_END_SCREEN_SIZE })
    }
}
