package sh.bab.plugin.run

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import sh.bab.plugin.BabBundle
import javax.swing.JComponent

class BabRunConfigurationSettingsEditor(
    private val project: Project
) : SettingsEditor<BabRunConfiguration>() {

    private val taskNameField = JBTextField()

    override fun resetEditorFrom(config: BabRunConfiguration) {
        taskNameField.text = config.taskName
    }

    override fun applyEditorTo(config: BabRunConfiguration) {
        config.taskName = taskNameField.text
    }

    override fun createEditor(): JComponent {
        return panel {
            row(BabBundle.message("run.configuration.task.name")) {
                cell(taskNameField)
                    .columns(COLUMNS_MEDIUM)
                    .align(AlignX.FILL)
            }
            row {
                link(BabBundle.message("run.configuration.settings.link")) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, BabBundle.message("settings.title"))
                }
            }
        }
    }
}
