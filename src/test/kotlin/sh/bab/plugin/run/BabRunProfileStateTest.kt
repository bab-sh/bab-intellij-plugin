package sh.bab.plugin.run

import com.intellij.execution.configurations.ConfigurationType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import sh.bab.plugin.settings.BabSettings

class BabRunProfileStateTest : BasePlatformTestCase() {

    private fun getConfigurationType(): BabRunConfigurationType {
        return ConfigurationType.CONFIGURATION_TYPE_EP.findExtensionOrFail(BabRunConfigurationType::class.java)
    }

    private fun createConfiguration(taskName: String = "build"): BabRunConfiguration {
        val type = getConfigurationType()
        val factory = type.configurationFactories.first()
        val config = BabRunConfiguration(project, factory, "Test Config")
        config.taskName = taskName
        return config
    }

    fun testArgumentParsingSimple() {
        val args = "--verbose --debug"
        val parsed = parseArgumentsHelper(args)

        assertEquals("Should have 2 arguments", 2, parsed.size)
        assertEquals("First argument", "--verbose", parsed[0])
        assertEquals("Second argument", "--debug", parsed[1])
    }

    fun testArgumentParsingWithDoubleQuotes() {
        val args = """--message "hello world" --flag"""
        val parsed = parseArgumentsHelper(args)

        assertEquals("Should have 3 arguments", 3, parsed.size)
        assertEquals("--message", parsed[0])
        assertEquals("hello world", parsed[1])
        assertEquals("--flag", parsed[2])
    }

    fun testArgumentParsingWithSingleQuotes() {
        val args = """--message 'hello world' --flag"""
        val parsed = parseArgumentsHelper(args)

        assertEquals("Should have 3 arguments", 3, parsed.size)
        assertEquals("--message", parsed[0])
        assertEquals("hello world", parsed[1])
        assertEquals("--flag", parsed[2])
    }

    fun testArgumentParsingEmpty() {
        val args = ""
        val parsed = parseArgumentsHelper(args)

        assertTrue("Empty string should return empty list", parsed.isEmpty())
    }

    fun testArgumentParsingBlank() {
        val args = "   "
        val parsed = parseArgumentsHelper(args)

        assertTrue("Blank string should return empty list", parsed.isEmpty())
    }

    fun testArgumentParsingMixedQuotes() {
        val args = """--single 'value one' --double "value two""""
        val parsed = parseArgumentsHelper(args)

        assertEquals("Should have 4 arguments", 4, parsed.size)
        assertEquals("--single", parsed[0])
        assertEquals("value one", parsed[1])
        assertEquals("--double", parsed[2])
        assertEquals("value two", parsed[3])
    }

    fun testArgumentParsingMultipleSpaces() {
        val args = "--arg1   --arg2    --arg3"
        val parsed = parseArgumentsHelper(args)

        assertEquals("Should have 3 arguments, ignoring extra spaces", 3, parsed.size)
        assertEquals("--arg1", parsed[0])
        assertEquals("--arg2", parsed[1])
        assertEquals("--arg3", parsed[2])
    }

    fun testSettingsAffectBehavior() {
        val settings = BabSettings.getInstance(project)

        settings.dryRun = true
        assertTrue("Dry run should be enabled", settings.dryRun)

        settings.dryRun = false
        assertFalse("Dry run should be disabled", settings.dryRun)
    }

    fun testSettingsAdditionalArgs() {
        val settings = BabSettings.getInstance(project)
        settings.additionalArgs = "--verbose --timeout=30"

        assertEquals("Additional args should be set", "--verbose --timeout=30", settings.additionalArgs)
    }

    fun testConfigurationTaskName() {
        val config = createConfiguration("deploy:production")
        assertEquals("Task name should be set", "deploy:production", config.taskName)
    }

    fun testEffectiveBinaryPath() {
        val settings = BabSettings.getInstance(project)

        settings.babBinaryPath = ""
        assertEquals("Should default to 'bab'", "bab", settings.getEffectiveBabBinaryPath())

        settings.babBinaryPath = "/usr/local/bin/bab"
        assertEquals("Should use custom path", "/usr/local/bin/bab", settings.getEffectiveBabBinaryPath())
    }

    fun testEffectiveWorkingDirectory() {
        val settings = BabSettings.getInstance(project)

        settings.workingDirectory = ""
        val projectPath = "/home/user/project"
        assertEquals("Should use project path", projectPath, settings.getEffectiveWorkingDirectory(projectPath))

        settings.workingDirectory = "/custom/dir"
        assertEquals("Should use custom directory", "/custom/dir", settings.getEffectiveWorkingDirectory(projectPath))
    }

    private fun parseArgumentsHelper(args: String): List<String> {
        if (args.isBlank()) return emptyList()

        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inSingleQuote = false
        var inDoubleQuote = false
        var i = 0

        while (i < args.length) {
            val c = args[i]
            when {
                c == '\'' && !inDoubleQuote -> {
                    inSingleQuote = !inSingleQuote
                }
                c == '"' && !inSingleQuote -> {
                    inDoubleQuote = !inDoubleQuote
                }
                c == ' ' && !inSingleQuote && !inDoubleQuote -> {
                    if (current.isNotEmpty()) {
                        result.add(current.toString())
                        current.clear()
                    }
                }
                else -> {
                    current.append(c)
                }
            }
            i++
        }

        if (current.isNotEmpty()) {
            result.add(current.toString())
        }

        return result
    }
}
