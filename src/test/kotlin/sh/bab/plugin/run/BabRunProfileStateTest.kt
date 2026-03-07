package sh.bab.plugin.run

import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ParametersList
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert.assertNotEquals
import sh.bab.plugin.settings.BabSettings

class BabRunProfileStateTest : BasePlatformTestCase() {

    private fun getConfigurationType(): BabRunConfigurationType {
        return ConfigurationType.CONFIGURATION_TYPE_EP.findExtensionOrFail(BabRunConfigurationType::class.java)
    }

    private fun createConfiguration(taskName: String): BabRunConfiguration {
        val type = getConfigurationType()
        val factory = type.configurationFactories.first()
        val config = BabRunConfiguration(project, factory, "Test Config")
        config.taskName = taskName
        return config
    }

    fun testArgumentParsingSimple() {
        val parsed = ParametersList.parse("--verbose --debug").toList()

        assertEquals("Should have 2 arguments", 2, parsed.size)
        assertEquals("First argument", "--verbose", parsed[0])
        assertEquals("Second argument", "--debug", parsed[1])
    }

    fun testArgumentParsingWithDoubleQuotes() {
        val parsed = ParametersList.parse("""--message "hello world" --flag""").toList()

        assertEquals("Should have 3 arguments", 3, parsed.size)
        assertEquals("--message", parsed[0])
        assertEquals("hello world", parsed[1])
        assertEquals("--flag", parsed[2])
    }

    fun testArgumentParsingWithSingleQuotes() {
        val parsed = ParametersList.parse("""--message "hello world" --flag""").toList()

        assertEquals("Should have 3 arguments", 3, parsed.size)
        assertEquals("--message", parsed[0])
        assertEquals("hello world", parsed[1])
        assertEquals("--flag", parsed[2])
    }

    fun testArgumentParsingEmpty() {
        val parsed = ParametersList.parse("").toList()
        assertTrue("Empty string should return empty list", parsed.isEmpty())
    }

    fun testArgumentParsingBlank() {
        val parsed = ParametersList.parse("   ").toList()
        assertTrue("Blank string should return empty list", parsed.isEmpty())
    }

    fun testArgumentParsingMixedQuotes() {
        val parsed = ParametersList.parse("""--first "value one" --second "value two"""").toList()

        assertEquals("Should have 4 arguments", 4, parsed.size)
        assertEquals("--first", parsed[0])
        assertEquals("value one", parsed[1])
        assertEquals("--second", parsed[2])
        assertEquals("value two", parsed[3])
    }

    fun testArgumentParsingMultipleSpaces() {
        val parsed = ParametersList.parse("--arg1   --arg2    --arg3").toList()

        assertEquals("Should have 3 arguments, ignoring extra spaces", 3, parsed.size)
        assertEquals("--arg1", parsed[0])
        assertEquals("--arg2", parsed[1])
        assertEquals("--arg3", parsed[2])
    }

    fun testSettingsAffectBehavior() {
        val settings = project.service<BabSettings>()
        val initialDryRun = settings.dryRun

        settings.dryRun = !initialDryRun
        assertNotEquals("Dry run should have toggled", initialDryRun, settings.dryRun)

        settings.dryRun = initialDryRun
        assertEquals("Dry run should be restored", initialDryRun, settings.dryRun)
    }

    fun testSettingsAdditionalArgs() {
        val settings = project.service<BabSettings>()
        settings.additionalArgs = "--verbose --timeout=30"

        assertEquals("Additional args should be set", "--verbose --timeout=30", settings.additionalArgs)
    }

    fun testConfigurationTaskName() {
        val config = createConfiguration("deploy:production")
        assertEquals("Task name should be set", "deploy:production", config.taskName)
    }

    fun testEffectiveBinaryPath() {
        val settings = project.service<BabSettings>()

        settings.babBinaryPath = ""
        assertEquals("Should default to 'bab'", "bab", settings.getEffectiveBabBinaryPath())

        settings.babBinaryPath = "/usr/local/bin/bab"
        assertEquals("Should use custom path", "/usr/local/bin/bab", settings.getEffectiveBabBinaryPath())
    }

    fun testEffectiveWorkingDirectory() {
        val settings = project.service<BabSettings>()

        settings.workingDirectory = ""
        val projectPath = "/home/user/project"
        assertEquals("Should use project path", projectPath, settings.getEffectiveWorkingDirectory(projectPath))

        settings.workingDirectory = "/custom/dir"
        assertEquals("Should use custom directory", "/custom/dir", settings.getEffectiveWorkingDirectory(projectPath))
    }
}
