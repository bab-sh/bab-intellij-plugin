package sh.bab.plugin.util

import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLSequenceItem

object BabPsiUtil {

    fun isInsideDepsField(element: PsiElement): Boolean {
        var parent: PsiElement? = element.parent
        while (parent != null) {
            when (parent) {
                is YAMLSequenceItem -> {
                    val keyValue = parent.parent?.parent
                    if (keyValue is YAMLKeyValue && keyValue.keyText == "deps") {
                        return true
                    }
                }
                is YAMLKeyValue -> {
                    if (parent.keyText == "deps") {
                        return true
                    }
                }
            }
            parent = parent.parent
        }
        return false
    }

    fun findCurrentTaskName(element: PsiElement): String? {
        var parent: PsiElement? = element.parent
        while (parent != null) {
            if (parent is YAMLKeyValue) {
                val grandparent = parent.parent
                if (grandparent is YAMLMapping) {
                    val greatGrandparent = grandparent.parent
                    if (greatGrandparent is YAMLKeyValue && greatGrandparent.keyText == "tasks") {
                        return parent.keyText
                    }
                }
            }
            parent = parent.parent
        }
        return null
    }

    fun getTasksMapping(file: YAMLFile): YAMLMapping? {
        val rootMapping = file.documents.firstOrNull()?.topLevelValue as? YAMLMapping
            ?: return null
        val tasksKeyValue = rootMapping.keyValues.find { it.keyText == "tasks" }
            ?: return null
        return tasksKeyValue.value as? YAMLMapping
    }

    fun extractTaskNames(file: YAMLFile): Set<String> {
        return getTasksMapping(file)
            ?.keyValues
            ?.mapNotNull { it.keyText }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            ?: emptySet()
    }

    fun getTaskKeyValues(file: YAMLFile): List<YAMLKeyValue> {
        return getTasksMapping(file)?.keyValues?.toList() ?: emptyList()
    }
}
