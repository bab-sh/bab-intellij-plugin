package sh.bab.plugin.run

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

class BabRunConfigurationOptions : RunConfigurationOptions() {

    private val taskNameProperty: StoredProperty<String?> = string("")
        .provideDelegate(this, "taskName")

    var taskName: String
        get() = taskNameProperty.getValue(this) ?: ""
        set(value) {
            taskNameProperty.setValue(this, value)
        }
}
