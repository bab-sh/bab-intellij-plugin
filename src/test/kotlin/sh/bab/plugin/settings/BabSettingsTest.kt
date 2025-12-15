package sh.bab.plugin.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert.assertNotEquals

class BabSettingsTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        val settings = BabSettings.getInstance(project)
        settings.babBinaryPath = ""
        settings.workingDirectory = ""
        settings.additionalArgs = ""
        settings.dryRun = false
    }

    fun testSettingsInstantiation() {
        val settings = BabSettings.getInstance(project)
        assertNotNull("BabSettings should be available", settings)
    }

    fun testDefaultValuesAfterReset() {
        val settings = BabSettings.getInstance(project)
        assertEquals("babBinaryPath should be empty after reset", "", settings.babBinaryPath)
        assertEquals("workingDirectory should be empty after reset", "", settings.workingDirectory)
        assertEquals("additionalArgs should be empty after reset", "", settings.additionalArgs)
        assertFalse("dryRun should be false after reset", settings.dryRun)
    }

    fun testGetEffectiveBabBinaryPathWithEmpty() {
        val settings = BabSettings.getInstance(project)
        settings.babBinaryPath = ""

        assertEquals("Empty path should default to 'bab'", "bab", settings.getEffectiveBabBinaryPath())
    }

    fun testGetEffectiveBabBinaryPathWithValue() {
        val settings = BabSettings.getInstance(project)
        settings.babBinaryPath = "/usr/local/bin/bab"

        assertEquals("Should return configured path", "/usr/local/bin/bab", settings.getEffectiveBabBinaryPath())
    }

    fun testGetEffectiveWorkingDirectoryWithEmpty() {
        val settings = BabSettings.getInstance(project)
        settings.workingDirectory = ""

        val projectPath = "/home/user/project"
        assertEquals("Empty directory should default to project path",
            projectPath, settings.getEffectiveWorkingDirectory(projectPath))
    }

    fun testGetEffectiveWorkingDirectoryWithValue() {
        val settings = BabSettings.getInstance(project)
        settings.workingDirectory = "/custom/path"

        assertEquals("Should return configured directory",
            "/custom/path", settings.getEffectiveWorkingDirectory("/home/user/project"))
    }

    fun testGetEffectiveWorkingDirectoryWithNullProjectPath() {
        val settings = BabSettings.getInstance(project)
        settings.workingDirectory = ""

        assertEquals("Null project path should return '.'",
            ".", settings.getEffectiveWorkingDirectory(null))
    }

    fun testSetBabBinaryPath() {
        val settings = BabSettings.getInstance(project)
        settings.babBinaryPath = "/opt/bab/bin/bab"

        assertEquals("/opt/bab/bin/bab", settings.babBinaryPath)
    }

    fun testSetWorkingDirectory() {
        val settings = BabSettings.getInstance(project)
        settings.workingDirectory = "/my/working/dir"

        assertEquals("/my/working/dir", settings.workingDirectory)
    }

    fun testSetAdditionalArgs() {
        val settings = BabSettings.getInstance(project)
        settings.additionalArgs = "--verbose --debug"

        assertEquals("--verbose --debug", settings.additionalArgs)
    }

    fun testSetDryRun() {
        val settings = BabSettings.getInstance(project)
        val initialValue = settings.dryRun

        settings.dryRun = !initialValue
        assertNotEquals("Value should have changed", initialValue, settings.dryRun)

        settings.dryRun = initialValue
        assertEquals("Value should be restored", initialValue, settings.dryRun)
    }

    fun testGetState() {
        val settings = BabSettings.getInstance(project)
        settings.babBinaryPath = "/test/path"
        settings.dryRun = true

        val state = settings.state
        assertNotNull("getState should not return null", state)
        assertEquals("State should reflect current babBinaryPath", "/test/path", state.babBinaryPath)
        assertTrue("State should reflect current dryRun", state.dryRun)
    }

    fun testLoadState() {
        val settings = BabSettings.getInstance(project)

        val newState = BabSettings()
        newState.babBinaryPath = "/new/path"
        newState.workingDirectory = "/new/dir"
        newState.additionalArgs = "--new-arg"
        newState.dryRun = true

        settings.loadState(newState)

        assertEquals("/new/path", settings.babBinaryPath)
        assertEquals("/new/dir", settings.workingDirectory)
        assertEquals("--new-arg", settings.additionalArgs)
        assertTrue(settings.dryRun)
    }

    fun testMultipleSettingsInstances() {
        val settings1 = BabSettings.getInstance(project)
        val settings2 = BabSettings.getInstance(project)

        assertSame("getInstance should return same instance", settings1, settings2)
    }

    fun testSettingsAfterModification() {
        val settings = BabSettings.getInstance(project)

        settings.babBinaryPath = "/first/path"
        assertEquals("/first/path", settings.babBinaryPath)

        settings.babBinaryPath = "/second/path"
        assertEquals("/second/path", settings.babBinaryPath)
    }

    fun testDetectAllBabBinariesReturnsEmptyListWhenNoneFound() {
        val executables = BabSettings.detectAllBabBinaries()
        assertNotNull("detectAllBabBinaries should return a list (not null)", executables)
    }

    fun testDetectBabBinaryReturnsNullWhenNoneFound() {
        BabSettings.detectBabBinary()
    }

    fun testBabExecutableToStringWithVersion() {
        val executable = BabExecutable("/usr/bin/bab", "1.2.3")
        assertEquals("/usr/bin/bab", executable.toString())
    }

    fun testBabExecutableToStringWithoutVersion() {
        val executable = BabExecutable("/usr/bin/bab", null)
        assertEquals("/usr/bin/bab", executable.toString())
    }

    fun testBabExecutableDataClassEquality() {
        val exec1 = BabExecutable("/usr/bin/bab", "1.0.0")
        val exec2 = BabExecutable("/usr/bin/bab", "1.0.0")
        val exec3 = BabExecutable("/usr/local/bin/bab", "1.0.0")

        assertEquals("Same path and version should be equal", exec1, exec2)
        assertNotEquals("Different paths should not be equal", exec1, exec3)
    }

    fun testBabExecutablePath() {
        val executable = BabExecutable("/custom/path/bab", "2.0.0")
        assertEquals("/custom/path/bab", executable.path)
        assertEquals("2.0.0", executable.version)
    }
}
