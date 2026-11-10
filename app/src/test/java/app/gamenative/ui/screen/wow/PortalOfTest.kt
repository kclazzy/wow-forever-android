package app.gamenative.ui.screen.wow

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PortalOfTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun configWtf(root: File) = File(root, "${WowClientDownloader.FLAVOR_DIR}/WTF/Config.wtf")

    private fun rootWithConfig(content: String): File {
        val root = tempFolder.newFolder()
        configWtf(root).apply { parentFile?.mkdirs() }.writeText(content)
        return root
    }

    @Test
    fun readsThePortalFromConfig() {
        assertEquals("EU", portalOf(rootWithConfig("SET portal \"EU\"\n")))
    }

    @Test
    fun withoutAnyFiles_defaultsToUs() {
        assertEquals("US", portalOf(tempFolder.newFolder()))
    }

    // Without "all files access" Android reports the file as existing but refuses to open it (EACCES). The
    // launcher screen asks for the portal on first composition, so this must not crash before the permission
    // dialog can be shown.
    @Test
    fun unreadableConfig_fallsBackInsteadOfCrashing() {
        val root = rootWithConfig("SET portal \"EU\"\n")
        configWtf(root).setReadable(false)
        assumeFalse("permissions are not enforced when running as root", configWtf(root).canRead())
        assertEquals("US", portalOf(root))
    }
}
