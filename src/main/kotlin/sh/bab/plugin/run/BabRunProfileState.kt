package sh.bab.plugin.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.ParametersList
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.terminal.TerminalExecutionConsole
import com.intellij.openapi.components.service
import sh.bab.plugin.settings.BabSettings
import java.io.File

class BabRunProfileState(
    private val configuration: BabRunConfiguration,
    environment: ExecutionEnvironment
) : CommandLineState(environment) {

    override fun startProcess(): ProcessHandler {
        val commandLine = buildCommandLine()
        val processHandler = KillableColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(processHandler)
        return processHandler
    }

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        val processHandler = startProcess()
        val console = TerminalExecutionConsole(environment.project, null)
        console.attachToProcess(processHandler)
        return DefaultExecutionResult(console, processHandler)
    }

    private fun buildCommandLine(): GeneralCommandLine {
        val project = configuration.project
        val settings = project.service<BabSettings>()

        return GeneralCommandLine().apply {
            exePath = settings.getEffectiveBabBinaryPath()
            workDirectory = File(settings.getEffectiveWorkingDirectory(project.basePath))

            if (settings.dryRun) {
                addParameter("--dry-run")
            }

            addParameter(configuration.taskName)

            ParametersList.parse(settings.additionalArgs).forEach { addParameter(it) }
        }
    }
}
