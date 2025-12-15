package sh.bab.plugin.run

import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class BabRunConfigurationTest : BasePlatformTestCase() {

    private fun getConfigurationType(): BabRunConfigurationType {
        return ConfigurationType.CONFIGURATION_TYPE_EP.findExtensionOrFail(BabRunConfigurationType::class.java)
    }

    private fun createConfiguration(name: String = "Test Config"): BabRunConfiguration {
        val type = getConfigurationType()
        val factory = type.configurationFactories.first()
        return BabRunConfiguration(project, factory, name)
    }

    fun testConfigurationTypeRegistration() {
        val type = getConfigurationType()
        assertNotNull("BabRunConfigurationType should be registered", type)
        assertEquals("Type ID should match", BabRunConfigurationType.ID, type.id)
    }

    fun testConfigurationTypeHasFactory() {
        val type = getConfigurationType()
        assertTrue("Should have at least one factory", type.configurationFactories.isNotEmpty())
    }

    fun testGetBabRunConfigurationType() {
        val instance = getBabRunConfigurationType()
        assertNotNull("getBabRunConfigurationType should return type", instance)
        assertEquals("Should return same type", BabRunConfigurationType.ID, instance.id)
    }

    fun testRunConfigurationCreation() {
        val config = createConfiguration("My Test Config")
        assertNotNull("Configuration should be created", config)
        assertEquals("Name should match", "My Test Config", config.name)
    }

    fun testTaskNameGetterSetter() {
        val config = createConfiguration()
        assertEquals("Default task name should be empty", "", config.taskName)

        config.taskName = "build"
        assertEquals("Task name should be set", "build", config.taskName)

        config.taskName = "deploy:production"
        assertEquals("Namespaced task name should work", "deploy:production", config.taskName)
    }

    fun testCheckConfigurationWithEmptyTask() {
        val config = createConfiguration()
        config.taskName = ""

        try {
            config.checkConfiguration()
            fail("Should throw RuntimeConfigurationError for empty task")
        } catch (e: RuntimeConfigurationError) {
            assertNotNull("Error message should not be null", e.message)
        }
    }

    fun testCheckConfigurationWithBlankTask() {
        val config = createConfiguration()
        config.taskName = "   "

        try {
            config.checkConfiguration()
            fail("Should throw RuntimeConfigurationError for blank task")
        } catch (_: RuntimeConfigurationError) {
        }
    }

    fun testCheckConfigurationWithValidTask() {
        val config = createConfiguration()
        config.taskName = "build"

        config.checkConfiguration()
    }

    fun testGetConfigurationEditor() {
        val config = createConfiguration()
        val editor = config.configurationEditor

        assertNotNull("Configuration editor should not be null", editor)
        assertTrue("Should be BabRunConfigurationSettingsEditor",
            editor is BabRunConfigurationSettingsEditor)
    }

    fun testConfigurationFactoryId() {
        val type = getConfigurationType()
        val factory = type.configurationFactories.first()

        assertEquals("Factory ID should match", "BabConfigurationFactory", factory.id)
    }

    fun testCreateTemplateConfiguration() {
        val type = getConfigurationType()
        val factory = type.configurationFactories.first()
        val template = factory.createTemplateConfiguration(project)

        assertNotNull("Template configuration should not be null", template)
        assertTrue("Should be BabRunConfiguration", template is BabRunConfiguration)
        assertEquals("Template name should be 'Bab Task'", "Bab Task", template.name)
    }

    fun testOptionsClass() {
        val type = getConfigurationType()
        val factory = type.configurationFactories.first() as BabConfigurationFactory

        assertEquals("Options class should be BabRunConfigurationOptions",
            BabRunConfigurationOptions::class.java, factory.optionsClass)
    }

    fun testTaskNamePersistence() {
        val config = createConfiguration()
        config.taskName = "test:unit"

        assertEquals("Task name should persist", "test:unit", config.taskName)

        config.taskName = "build:all"
        assertEquals("Task name should be updated", "build:all", config.taskName)
    }
}
