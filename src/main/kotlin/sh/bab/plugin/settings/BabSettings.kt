package sh.bab.plugin.settings

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.xmlb.XmlSerializerUtil
import java.io.File

data class BabExecutable(
    val path: String,
    val version: String?
) {
    override fun toString(): String = path
}

@State(name = "BabSettings", storages = [Storage("bab.xml")])
@Service(Service.Level.PROJECT)
class BabSettings : PersistentStateComponent<BabSettings> {

    var babBinaryPath: String = ""
    var workingDirectory: String = ""
    var additionalArgs: String = ""
    var dryRun: Boolean = false

    override fun getState(): BabSettings = this

    override fun loadState(state: BabSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun getEffectiveBabBinaryPath(): String = babBinaryPath.ifEmpty { "bab" }

    fun getEffectiveWorkingDirectory(projectBasePath: String?): String =
        workingDirectory.ifEmpty { projectBasePath ?: "." }

    companion object {
        private val EXECUTABLE_NAME = if (SystemInfo.isWindows) "bab.exe" else "bab"

        fun getInstance(project: Project): BabSettings =
            project.getService(BabSettings::class.java)

        fun detectBabBinary(): String? =
            detectAllBabBinaries().firstOrNull()?.path

        fun detectAllBabBinaries(): List<BabExecutable> {
            val paths = mutableSetOf<String>()
            val executables = mutableListOf<BabExecutable>()

            PathEnvironmentVariableUtil.findAllExeFilesInPath(EXECUTABLE_NAME)
                .filter { it.canExecute() }
                .forEach { file ->
                    val path = file.absolutePath
                    if (paths.add(path)) {
                        val version = getBabVersion(path).getOrNull()
                        executables.add(BabExecutable(path, version))
                    }
                }

            findAllInCommonLocations().forEach { path ->
                if (paths.add(path)) {
                    val version = getBabVersion(path).getOrNull()
                    executables.add(BabExecutable(path, version))
                }
            }

            return executables
        }

        private fun findAllInCommonLocations(): List<String> {
            val userHome = System.getProperty("user.home") ?: return emptyList()

            return buildList {
                if (!SystemInfo.isWindows) {
                    add(File("/usr/bin/$EXECUTABLE_NAME"))
                    add(File("/usr/local/bin/$EXECUTABLE_NAME"))
                    add(File(userHome, ".local/bin/$EXECUTABLE_NAME"))
                }

                if (SystemInfo.isLinux) {
                    add(File("/snap/bin/bab-sh"))
                    add(File("/home/linuxbrew/.linuxbrew/bin/$EXECUTABLE_NAME"))
                    add(File(userHome, ".linuxbrew/bin/$EXECUTABLE_NAME"))
                }

                if (SystemInfo.isMac) {
                    add(File("/opt/homebrew/bin/$EXECUTABLE_NAME"))
                }

                if (SystemInfo.isWindows) {
                    System.getenv("LOCALAPPDATA")?.let {
                        add(File(it, "bab/bin/$EXECUTABLE_NAME"))
                    }
                    System.getenv("USERPROFILE")?.let {
                        add(File(it, "scoop/shims/$EXECUTABLE_NAME"))
                    }
                    add(File("C:/ProgramData/chocolatey/bin/$EXECUTABLE_NAME"))
                }
            }.filter { it.exists() && it.canExecute() }
                .map { it.absolutePath }
        }

        fun getBabVersion(binaryPath: String): Result<String> {
            val effectivePath = binaryPath.ifEmpty { "bab" }

            if (binaryPath.isNotEmpty() && !File(binaryPath).exists()) {
                return Result.failure(Exception("Binary not found"))
            }

            return try {
                val commandLine = GeneralCommandLine(effectivePath, "--version")
                val handler = CapturingProcessHandler(commandLine)
                val result = handler.runProcess(5000)

                if (result.exitCode == 0 && result.stdout.isNotBlank()) {
                    Result.success(result.stdout.trim().lines().first())
                } else {
                    Result.failure(Exception(result.stderr.ifBlank { "Command failed" }))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
