package sh.bab.plugin.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import sh.bab.plugin.filetype.isBabfile
import sh.bab.plugin.services.BabFileService
import sh.bab.plugin.util.BabPsiUtil

class BabRunConfigurationProducer : LazyRunConfigurationProducer<BabRunConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory =
        getBabRunConfigurationType().configurationFactories.firstOrNull()
            ?: error("No configuration factory found for BabRunConfigurationType")

    override fun createConfigurationFromContext(context: ConfigurationContext): ConfigurationFromContext? {
        val result = super.createConfigurationFromContext(context) ?: return null
        val configuration = result.configuration as? BabRunConfiguration ?: return result
        val parts = configuration.taskName.split(":")

        if (parts.size > 1) {
            result.configurationSettings.folderName = parts.dropLast(1).joinToString(":")
        }

        return result
    }

    override fun setupConfigurationFromContext(
        configuration: BabRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val fullTaskName = resolveTaskName(context) ?: return false

        configuration.taskName = fullTaskName
        configuration.name = fullTaskName.split(":").last()

        return true
    }

    override fun isConfigurationFromContext(
        configuration: BabRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val fullTaskName = resolveTaskName(context) ?: return false
        return configuration.taskName == fullTaskName
    }

    private fun resolveTaskName(context: ConfigurationContext): String? {
        val element = context.psiLocation ?: return null
        val file = element.containingFile?.virtualFile ?: return null

        if (!isBabfile(file)) return null

        val localTaskName = BabPsiUtil.findCurrentTaskName(element) ?: return null
        val babFileService = context.project.service<BabFileService>()

        return babFileService.getFullTaskName(file, localTaskName) ?: localTaskName
    }
}
