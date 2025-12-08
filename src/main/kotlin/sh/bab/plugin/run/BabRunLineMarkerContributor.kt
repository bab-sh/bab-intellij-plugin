package sh.bab.plugin.run

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import sh.bab.plugin.BabBundle
import sh.bab.plugin.filetype.isBabfile
import sh.bab.plugin.services.BabFileService
import sh.bab.plugin.util.YamlKeys
import java.util.function.Function

class BabRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        if (element.parent !is YAMLKeyValue) return null

        val keyValue = element.parent as YAMLKeyValue

        val key = keyValue.key ?: return null
        if (!PsiTreeUtil.isAncestor(key, element, false)) return null

        val file = element.containingFile?.virtualFile ?: return null
        if (!isBabfile(file)) return null

        val parent = keyValue.parent
        if (parent !is YAMLMapping) return null

        val grandparent = parent.parent
        if (grandparent !is YAMLKeyValue || grandparent.keyText != YamlKeys.TASKS) return null

        val localTaskName = keyValue.keyText
        if (localTaskName.isEmpty()) return null

        val babFileService = element.project.service<BabFileService>()
        val fullTaskName = babFileService.getFullTaskName(file, localTaskName) ?: localTaskName

        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            ExecutorAction.getActions(),
            Function { BabBundle.message("run.line.marker.tooltip", fullTaskName) }
        )
    }
}
