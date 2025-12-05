package sh.bab.plugin.util

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequenceItem

object BabPsiUtil {

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
        val babfileValue = includeMapping.keyValues.find { it.keyText == "babfile" }?.value as? YAMLScalar
        return babfileValue?.textValue
    }

    fun resolveIncludedFile(file: YAMLFile, includeName: String): VirtualFile? {
        val path = getIncludeBabfilePath(file, includeName) ?: return null
        val parentDir = file.virtualFile?.parent ?: return null
        return if (path.startsWith("/")) {
            VirtualFileManager.getInstance().findFileByUrl("file://$path")
        } else {
            parentDir.findFileByRelativePath(path)
        }
    }

    fun getIncludedYamlFile(file: YAMLFile, includeName: String): YAMLFile? {
        val virtualFile = resolveIncludedFile(file, includeName) ?: return null
        return PsiManager.getInstance(file.project).findFile(virtualFile) as? YAMLFile
    }

    fun extractAllTaskReferences(file: YAMLFile): Set<String> {
        val result = mutableSetOf<String>()
        result.addAll(extractTaskNames(file))
        for (includeName in extractIncludeNames(file)) {
            val includedFile = getIncludedYamlFile(file, includeName) ?: continue
            for (taskName in extractTaskNames(includedFile)) {
                result.add("$includeName:$taskName")
            }
        }
        return result
    }

    fun resolveTaskReference(file: YAMLFile, reference: String): YAMLKeyValue? {
        getTaskKeyValues(file).find { it.keyText == reference }?.let { return it }
        if (reference.contains(":")) {
            val prefix = reference.substringBefore(":")
            val taskName = reference.substringAfter(":")
            val includedFile = getIncludedYamlFile(file, prefix) ?: return null
            return getTaskKeyValues(includedFile).find { it.keyText == taskName }
        }
        return null
    }

    fun isValidTaskReference(file: YAMLFile, reference: String): Boolean {
        return resolveTaskReference(file, reference) != null
    }
}