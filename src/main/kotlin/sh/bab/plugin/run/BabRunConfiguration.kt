package sh.bab.plugin.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import sh.bab.plugin.BabBundle

class BabRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : RunConfigurationBase<BabRunConfigurationOptions>(project, factory, name) {

    override fun getOptions(): BabRunConfigurationOptions {
        return super.getOptions() as BabRunConfigurationOptions
    }

    var taskName: String
        get() = options.taskName
        set(value) {
            options.taskName = value
        }

    override fun getConfigurationEditor(): SettingsEditor<BabRunConfiguration> {
        return BabRunConfigurationSettingsEditor(project)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return BabRunProfileState(this, environment)
    }

    override fun checkConfiguration() {
        if (taskName.isBlank()) {
            throw RuntimeConfigurationError(BabBundle.message("run.configuration.error.no.task"))
        }
    }
}
