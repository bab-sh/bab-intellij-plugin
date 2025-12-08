package sh.bab.plugin.settings

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.builder.*
import sh.bab.plugin.BabBundle
import java.io.File
import javax.swing.ComboBoxEditor
import javax.swing.DefaultComboBoxModel
import javax.swing.JList
import javax.swing.JTextField
import javax.swing.plaf.basic.BasicComboBoxEditor

class BabSettingsConfigurable(
    private val project: Project
) : BoundConfigurable(BabBundle.message("settings.title")) {

    private val settings = BabSettings.getInstance(project)
    private val comboBoxModel = DefaultComboBoxModel<BabExecutable>()
    private var currentPath = settings.babBinaryPath
    private lateinit var workingDirectoryField: ExtendableTextField

    override fun createPanel(): DialogPanel = panel {
        group(BabBundle.message("settings.group.installation")) {
            row(BabBundle.message("settings.bab.binary")) {
                populateExecutables()

                comboBox(comboBoxModel)
                    .applyToComponent {
                        renderer = createBabExecutableRenderer()
                        isEditable = true
                        editor = createBrowseEditor()
                        if (currentPath.isNotEmpty()) {
                            setSelectedPath(currentPath)
                        }
                    }
                    .align(AlignX.FILL)
                    .resizableColumn()
                    .validationOnInput {
                        validateBinaryPath(getSelectedPath())
                    }
                    .validationOnApply {
                        validateBinaryPath(getSelectedPath())
                    }
                    .onChanged {
                        currentPath = getSelectedPath()
                    }
            }

            row(BabBundle.message("settings.working.directory")) {
                workingDirectoryField = createWorkingDirectoryField()
                cell(workingDirectoryField)
                    .align(AlignX.FILL)
                    .resizableColumn()
                    .comment(BabBundle.message("settings.working.directory.comment", project.basePath ?: ""))
            }
        }

        group(BabBundle.message("settings.group.execution")) {
            row(BabBundle.message("settings.additional.args")) {
                textField()
                    .align(AlignX.FILL)
                    .bindText(settings::additionalArgs)
                    .comment(BabBundle.message("settings.additional.args.comment"))
            }

            row {
                checkBox(BabBundle.message("settings.dry.run"))
                    .bindSelected(settings::dryRun)
                    .comment(BabBundle.message("settings.dry.run.comment"))
            }
        }
    }

    private fun createWorkingDirectoryField(): ExtendableTextField {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle(BabBundle.message("settings.working.directory.browse.dialog.title"))

        val browseExtension = ExtendableTextComponent.Extension.create(
            AllIcons.General.OpenDisk,
            AllIcons.General.OpenDiskHover,
            BabBundle.message("settings.working.directory.browse.tooltip")
        ) {
            FileChooser.chooseFile(descriptor, project, null) { file ->
                workingDirectoryField.text = file.path
            }
        }

        return ExtendableTextField().apply {
            addExtension(browseExtension)
            text = settings.workingDirectory
        }
    }

    private fun extractVersion(versionOutput: String): String? {
        val regex = Regex("""v?\d+\.\d+\.\d+""")
        return regex.find(versionOutput)?.value?.let {
            if (it.startsWith("v")) it else "v$it"
        }
    }

    private fun createBabExecutableRenderer() = object : ColoredListCellRenderer<BabExecutable>() {
        override fun customizeCellRenderer(
            list: JList<out BabExecutable>,
            value: BabExecutable?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            if (value == null) return

            append(value.path)
            value.version?.let { version ->
                val versionDisplay = extractVersion(version)
                if (versionDisplay != null) {
                    append("  $versionDisplay", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
            }
        }
    }

    private fun createBrowseEditor(): ComboBoxEditor {
        val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
            .withTitle(BabBundle.message("settings.bab.binary.browse.dialog.title"))

        val browseExtension = ExtendableTextComponent.Extension.create(
            AllIcons.General.OpenDisk,
            AllIcons.General.OpenDiskHover,
            BabBundle.message("settings.bab.binary.browse.tooltip")
        ) {
            FileChooser.chooseFile(descriptor, project, null) { file ->
                currentPath = file.path
                setSelectedPath(file.path)
            }
        }

        return object : BasicComboBoxEditor() {
            override fun createEditorComponent(): JTextField {
                return ExtendableTextField().apply {
                    addExtension(browseExtension)
                    border = null
                }
            }
        }
    }

    private fun populateExecutables() {
        comboBoxModel.removeAllElements()
        BabSettings.detectAllBabBinaries().forEach { comboBoxModel.addElement(it) }
    }

    private fun setSelectedPath(path: String) {
        for (i in 0 until comboBoxModel.size) {
            val executable = comboBoxModel.getElementAt(i)
            if (executable.path == path) {
                comboBoxModel.selectedItem = executable
                return
            }
        }
        comboBoxModel.selectedItem = path
    }

    private fun getSelectedPath(): String {
        return when (val selected = comboBoxModel.selectedItem) {
            is BabExecutable -> selected.path
            is String -> selected
            else -> ""
        }
    }

    private fun validateBinaryPath(path: String): ValidationInfo? {
        if (path.isEmpty()) return null

        val file = File(path)
        if (!file.exists()) {
            return ValidationInfo(BabBundle.message("settings.validation.binary.not.found"))
        }
        if (!file.canExecute()) {
            return ValidationInfo(BabBundle.message("settings.validation.binary.not.executable"))
        }

        val versionResult = BabSettings.getBabVersion(path)
        if (versionResult.isFailure) {
            return ValidationInfo(
                BabBundle.message(
                    "settings.validation.binary.failed",
                    versionResult.exceptionOrNull()?.message ?: "Unknown error"
                )
            )
        }
        return null
    }

    override fun isModified(): Boolean =
        currentPath != settings.babBinaryPath ||
        workingDirectoryField.text != settings.workingDirectory ||
        super.isModified()

    override fun apply() {
        settings.babBinaryPath = currentPath
        settings.workingDirectory = workingDirectoryField.text
        super.apply()
    }

    override fun reset() {
        super.reset()
        currentPath = settings.babBinaryPath
        setSelectedPath(currentPath)
        workingDirectoryField.text = settings.workingDirectory
    }
}
