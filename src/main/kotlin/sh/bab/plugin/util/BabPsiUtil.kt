package sh.bab.plugin.util

import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequenceItem

data class TaskReference(val includePrefix: String?, val taskName: String)

object BabPsiUtil {

    fun parseTaskReference(reference: String): TaskReference {
        val parts = reference.split(":", limit = 2)
        return if (parts.size == 2) {
            TaskReference(includePrefix = parts[0], taskName = parts[1])
        } else {
            TaskReference(includePrefix = null, taskName = reference)
        }
    }

    fun isInsideDepsField(element: PsiElement): Boolean {
        var current: PsiElement? = element
        while (current != null) {
            if (current is YAMLSequenceItem) {
                val sequence = current.parent ?: run { current = current?.parent; continue }
                val depsKeyValue = sequence.parent as? YAMLKeyValue ?: run { current = current?.parent; continue }
                if (depsKeyValue.keyText != "deps") { current = current?.parent; continue }

                val taskMapping = depsKeyValue.parent as? YAMLMapping ?: run { current = current?.parent; continue }
                val taskKeyValue = taskMapping.parent as? YAMLKeyValue ?: run { current = current?.parent; continue }
                val tasksMapping = taskKeyValue.parent as? YAMLMapping ?: run { current = current?.parent; continue }
                val tasksKeyValue = tasksMapping.parent as? YAMLKeyValue ?: run { current = current?.parent; continue }

                if (tasksKeyValue.keyText == "tasks") {
                    return true
                }
            }
            current = current?.parent
        }
        return false
    }

    fun isInsideRunTaskField(element: PsiElement): Boolean {
        var current: PsiElement? = element
        while (current != null) {
            if (current is YAMLKeyValue && current.keyText == "task") {
                val mapping = current.parent as? YAMLMapping ?: run { current = current?.parent; continue }
                val sequenceItem = mapping.parent as? YAMLSequenceItem ?: run { current = current?.parent; continue }
                val sequence = sequenceItem.parent ?: run { current = current?.parent; continue }
                val runKeyValue = sequence.parent as? YAMLKeyValue ?: run { current = current?.parent; continue }
                if (runKeyValue.keyText != "run") { current = current?.parent; continue }

                val taskMapping = runKeyValue.parent as? YAMLMapping ?: run { current = current?.parent; continue }
                val taskKeyValue = taskMapping.parent as? YAMLKeyValue ?: run { current = current?.parent; continue }
                val tasksMapping = taskKeyValue.parent as? YAMLMapping ?: run { current = current?.parent; continue }
                val tasksKeyValue = tasksMapping.parent as? YAMLKeyValue ?: run { current = current?.parent; continue }

                if (tasksKeyValue.keyText == "tasks") {
                    return true
                }
            }
            current = current?.parent
        }
        return false
    }

    fun isTaskReferenceContext(element: PsiElement): Boolean {
        return isInsideDepsField(element) || isInsideRunTaskField(element)
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

    fun getIncludesMapping(file: YAMLFile): YAMLMapping? {
        val rootMapping = file.documents.firstOrNull()?.topLevelValue as? YAMLMapping
            ?: return null
        val includesKeyValue = rootMapping.keyValues.find { it.keyText == "includes" }
            ?: return null
        return includesKeyValue.value as? YAMLMapping
    }

    fun extractIncludeNames(file: YAMLFile): Set<String> {
        return getIncludesMapping(file)
            ?.keyValues
            ?.mapNotNull { it.keyText }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            ?: emptySet()
    }

    fun getIncludeBabfilePath(file: YAMLFile, includeName: String): String? {
        val includesMapping = getIncludesMapping(file) ?: return null
        val includeKeyValue = includesMapping.keyValues.find { it.keyText == includeName } ?: return null
        val includeMapping = includeKeyValue.value as? YAMLMapping ?: return null
        val babfileKeyValue = includeMapping.keyValues.find { it.keyText == "babfile" } ?: return null
        return (babfileKeyValue.value as? YAMLScalar)?.textValue
    }
}
