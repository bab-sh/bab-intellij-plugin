package sh.bab.plugin.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import sh.bab.plugin.BabBundle
import sh.bab.plugin.icons.BabIcons

fun getBabRunConfigurationType(): BabRunConfigurationType =
    ConfigurationType.CONFIGURATION_TYPE_EP.findExtensionOrFail(BabRunConfigurationType::class.java)

class BabRunConfigurationType : ConfigurationTypeBase(
    ID,
    BabBundle.message("run.configuration.type.name"),
    BabBundle.message("run.configuration.type.description"),
    BabIcons.Task
) {
    init {
        addFactory(BabConfigurationFactory(this))
    }

    companion object {
        const val ID = "BabRunConfiguration"
    }
}

class BabConfigurationFactory(type: BabRunConfigurationType) : ConfigurationFactory(type) {

    override fun getId(): String = "BabConfigurationFactory"

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return BabRunConfiguration(project, this, "Bab Task")
    }

    override fun getOptionsClass(): Class<out BabRunConfigurationOptions> {
        return BabRunConfigurationOptions::class.java
    }
}
