package sh.bab.plugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

data class BabSettingsState(
    var babBinaryPath: String = "",
    var workingDirectory: String = "",
    var additionalArgs: String = "",
    var dryRun: Boolean = false
)

@State(name = "BabSettings", storages = [Storage("bab.xml")])
@Service(Service.Level.PROJECT)
class BabSettings : PersistentStateComponent<BabSettingsState> {

    private var myState = BabSettingsState()

    var babBinaryPath: String
        get() = myState.babBinaryPath
        set(value) { myState.babBinaryPath = value }

    var workingDirectory: String
        get() = myState.workingDirectory
        set(value) { myState.workingDirectory = value }

    var additionalArgs: String
        get() = myState.additionalArgs
        set(value) { myState.additionalArgs = value }

    var dryRun: Boolean
        get() = myState.dryRun
        set(value) { myState.dryRun = value }

    override fun getState(): BabSettingsState = myState

    override fun loadState(state: BabSettingsState) {
        myState = state.copy()
    }

    fun getEffectiveBabBinaryPath(): String = babBinaryPath.ifEmpty { "bab" }

    fun getEffectiveWorkingDirectory(projectBasePath: String?): String =
        workingDirectory.ifEmpty { projectBasePath ?: "." }
}
