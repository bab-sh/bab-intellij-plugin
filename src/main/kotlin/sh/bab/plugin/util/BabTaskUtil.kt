package sh.bab.plugin.util

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import sh.bab.plugin.model.TaskReference
import sh.bab.plugin.model.YamlKeys

object BabTaskUtil {

    fun getTasksMapping(file: YAMLFile): YAMLMapping? {
        val rootMapping = file.documents.firstOrNull()?.topLevelValue as? YAMLMapping
            ?: return null
        val tasksKeyValue = rootMapping.keyValues.find { it.keyText == YamlKeys.TASKS }
            ?: return null
        return tasksKeyValue.value as? YAMLMapping
    }

    fun findCurrentTaskName(element: PsiElement): String? {
        val taskKeyValue = findCurrentTaskKeyValue(element) ?: return null
        return taskKeyValue.keyText
    }

    fun findCurrentTaskAliases(element: PsiElement): Set<String> {
        val taskKeyValue = findCurrentTaskKeyValue(element) ?: return emptySet()
        val taskMapping = taskKeyValue.value as? YAMLMapping ?: return emptySet()
        return extractAliasesFromTaskMapping(taskMapping).toSet()
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
        val babfileValue = includeMapping.keyValues.find { it.keyText == YamlKeys.BABFILE }?.value as? YAMLScalar
        return babfileValue?.textValue
    }

    fun extractAllTaskReferences(file: YAMLFile, prefix: String = "", visited: MutableSet<String> = mutableSetOf()): Set<String> {
        return extractAllTaskReferencesWithType(file, prefix, visited).map { it.reference }.toSet()
    }

    fun extractAllTaskReferencesWithType(file: YAMLFile, prefix: String = "", visited: MutableSet<String> = mutableSetOf()): List<TaskReference> {
        val filePath = file.virtualFile?.path ?: return emptyList()
        if (filePath in visited) return emptyList()
        visited.add(filePath)

        val result = mutableListOf<TaskReference>()

        for (taskRef in extractTaskReferences(file)) {
            val fullRef = if (prefix.isEmpty()) taskRef.reference else "$prefix:${taskRef.reference}"
            result.add(TaskReference(fullRef, taskRef.isAlias))
        }

        for (includeName in extractIncludeNames(file)) {
            val includedFile = getIncludedYamlFile(file, includeName) ?: continue
            val nestedPrefix = if (prefix.isEmpty()) includeName else "$prefix:$includeName"
            result.addAll(extractAllTaskReferencesWithType(includedFile, nestedPrefix, visited))
        }

        return result
    }

    fun resolveTaskReference(file: YAMLFile, reference: String, visited: MutableSet<String> = mutableSetOf()): YAMLKeyValue? {
        val filePath = file.virtualFile?.path ?: return null
        if (filePath in visited) return null
        visited.add(filePath)

        getTaskKeyValues(file).find { it.keyText == reference }?.let { return it }

        for (taskKeyValue in getTaskKeyValues(file)) {
            val taskMapping = taskKeyValue.value as? YAMLMapping ?: continue
            val aliases = extractAliasesFromTaskMapping(taskMapping)
            if (reference in aliases) {
                return taskKeyValue
            }
        }

        if (reference.contains(":")) {
            val prefix = reference.substringBefore(":")
            val remainder = reference.substringAfter(":")
            val includedFile = getIncludedYamlFile(file, prefix) ?: return null
            return resolveTaskReference(includedFile, remainder, visited)
        }

        return null
    }

    fun isValidTaskReference(file: YAMLFile, reference: String): Boolean {
        return resolveTaskReference(file, reference) != null
    }

    fun extractAliasesFromTaskMapping(taskMapping: YAMLMapping): List<String> {
        val result = mutableListOf<String>()

        val alias = taskMapping.keyValues.find { it.keyText == YamlKeys.ALIAS }?.valueText
        if (!alias.isNullOrEmpty()) {
            result.add(alias)
        }

        val aliasesSequence = taskMapping.keyValues.find { it.keyText == YamlKeys.ALIASES }?.value as? YAMLSequence
        aliasesSequence?.items?.mapNotNull { it.value?.text }?.let { result.addAll(it) }

        return result
    }

    private fun findCurrentTaskKeyValue(element: PsiElement): YAMLKeyValue? {
        var parent: PsiElement? = element.parent
        while (parent != null) {
            if (parent is YAMLKeyValue) {
                val grandparent = parent.parent
                if (grandparent is YAMLMapping) {
                    val greatGrandparent = grandparent.parent
                    if (greatGrandparent is YAMLKeyValue && greatGrandparent.keyText == YamlKeys.TASKS) {
                        return parent
                    }
                }
            }
            parent = parent.parent
        }
        return null
    }

    private fun extractTaskReferences(file: YAMLFile): List<TaskReference> {
        val result = mutableListOf<TaskReference>()
        val tasksMapping = getTasksMapping(file) ?: return result

        for (taskKeyValue in tasksMapping.keyValues) {
            val taskName = taskKeyValue.keyText
            if (taskName.isNotEmpty()) {
                result.add(TaskReference(taskName, isAlias = false))
            }

            val taskMapping = taskKeyValue.value as? YAMLMapping ?: continue
            for (alias in extractAliasesFromTaskMapping(taskMapping)) {
                result.add(TaskReference(alias, isAlias = true))
            }
        }

        return result
    }

    private fun getTaskKeyValues(file: YAMLFile): List<YAMLKeyValue> {
        return getTasksMapping(file)?.keyValues?.toList() ?: emptyList()
    }

    private fun getIncludesMapping(file: YAMLFile): YAMLMapping? {
        val rootMapping = file.documents.firstOrNull()?.topLevelValue as? YAMLMapping
            ?: return null
        val includesKeyValue = rootMapping.keyValues.find { it.keyText == YamlKeys.INCLUDES }
            ?: return null
        return includesKeyValue.value as? YAMLMapping
    }

    private fun resolveIncludedFile(file: YAMLFile, includeName: String): VirtualFile? {
        val path = getIncludeBabfilePath(file, includeName) ?: return null
        val parentDir = file.virtualFile?.parent ?: return null
        return if (path.startsWith("/")) {
            VirtualFileManager.getInstance().findFileByUrl("file://$path")
        } else {
            parentDir.findFileByRelativePath(path)
        }
    }

    private fun getIncludedYamlFile(file: YAMLFile, includeName: String): YAMLFile? {
        val virtualFile = resolveIncludedFile(file, includeName) ?: return null
        return PsiManager.getInstance(file.project).findFile(virtualFile) as? YAMLFile
    }
}
