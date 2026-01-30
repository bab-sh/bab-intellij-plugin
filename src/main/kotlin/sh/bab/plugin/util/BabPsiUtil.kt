package sh.bab.plugin.util

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem

object YamlKeys {
    const val TASKS = "tasks"
    const val DEPS = "deps"
    const val RUN = "run"
    const val INCLUDES = "includes"
    const val BABFILE = "babfile"
    const val DESC = "desc"
    const val ALIAS = "alias"
    const val ALIASES = "aliases"
}

object BabPsiUtil {

    private fun isUnderTasksKey(mapping: YAMLMapping): Boolean {
        val taskKeyValue = mapping.parent as? YAMLKeyValue ?: return false
        val tasksMapping = taskKeyValue.parent as? YAMLMapping ?: return false
        val tasksKeyValue = tasksMapping.parent as? YAMLKeyValue ?: return false
        return tasksKeyValue.keyText == YamlKeys.TASKS
    }

    private fun isInsideDepsField(element: PsiElement): Boolean {
        var current: PsiElement? = element
        while (current != null) {
            if (current is YAMLSequenceItem) {
                if (checkDepsHierarchy(current)) return true
            }
            current = current.parent
        }
        return false
    }

    private fun checkDepsHierarchy(sequenceItem: YAMLSequenceItem): Boolean {
        val sequence = sequenceItem.parent ?: return false
        val depsKeyValue = sequence.parent as? YAMLKeyValue ?: return false
        if (depsKeyValue.keyText != YamlKeys.DEPS) return false

        val taskMapping = depsKeyValue.parent as? YAMLMapping ?: return false
        return isUnderTasksKey(taskMapping)
    }

    private fun isInsideRunTaskField(element: PsiElement): Boolean {
        var current: PsiElement? = element
        while (current != null) {
            if (current is YAMLKeyValue && current.keyText == "task") {
                if (checkRunTaskHierarchy(current)) return true
            }
            current = current.parent
        }
        return false
    }

    private fun checkRunTaskHierarchy(taskKeyValue: YAMLKeyValue): Boolean {
        val mapping = taskKeyValue.parent as? YAMLMapping ?: return false
        val sequenceItem = mapping.parent as? YAMLSequenceItem ?: return false
        val sequence = sequenceItem.parent ?: return false
        val runKeyValue = sequence.parent as? YAMLKeyValue ?: return false
        if (runKeyValue.keyText != YamlKeys.RUN) return false

        val taskMapping = runKeyValue.parent as? YAMLMapping ?: return false
        return isUnderTasksKey(taskMapping)
    }

    fun isTaskReferenceContext(element: PsiElement): Boolean {
        return isInsideDepsField(element) || isInsideRunTaskField(element)
    }

    fun isIncludeBabfileContext(element: PsiElement): Boolean {
        var current: PsiElement? = element
        while (current != null) {
            if (current is YAMLKeyValue && current.keyText == YamlKeys.BABFILE) {
                if (checkIncludeHierarchy(current)) return true
            }
            current = current.parent
        }
        return false
    }

    private fun checkIncludeHierarchy(babfileKeyValue: YAMLKeyValue): Boolean {
        val includeMapping = babfileKeyValue.parent as? YAMLMapping ?: return false
        val includeKeyValue = includeMapping.parent as? YAMLKeyValue ?: return false
        val includesMapping = includeKeyValue.parent as? YAMLMapping ?: return false
        val includesKeyValue = includesMapping.parent as? YAMLKeyValue ?: return false

        return includesKeyValue.keyText == YamlKeys.INCLUDES
    }

    fun isDirContext(element: PsiElement): Boolean {
        var current: PsiElement? = element
        while (current != null) {
            if (current is YAMLKeyValue && current.keyText == "dir") {
                return isRootDirContext(current) || isTaskDirContext(current) || isRunItemDirContext(current)
            }
            current = current.parent
        }
        return false
    }

    private fun isRootDirContext(dirKeyValue: YAMLKeyValue): Boolean {
        val rootMapping = dirKeyValue.parent as? YAMLMapping ?: return false
        return rootMapping.parent is YAMLDocument
    }

    private fun isTaskDirContext(dirKeyValue: YAMLKeyValue): Boolean {
        val taskMapping = dirKeyValue.parent as? YAMLMapping ?: return false
        return isUnderTasksKey(taskMapping)
    }

    private fun isRunItemDirContext(dirKeyValue: YAMLKeyValue): Boolean {
        val runItemMapping = dirKeyValue.parent as? YAMLMapping ?: return false
        val sequenceItem = runItemMapping.parent as? YAMLSequenceItem ?: return false
        val sequence = sequenceItem.parent ?: return false
        val runKeyValue = sequence.parent as? YAMLKeyValue ?: return false
        if (runKeyValue.keyText != YamlKeys.RUN) return false
        val taskMapping = runKeyValue.parent as? YAMLMapping ?: return false
        return isUnderTasksKey(taskMapping)
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

    private fun getTasksMapping(file: YAMLFile): YAMLMapping? {
        val rootMapping = file.documents.firstOrNull()?.topLevelValue as? YAMLMapping
            ?: return null
        val tasksKeyValue = rootMapping.keyValues.find { it.keyText == YamlKeys.TASKS }
            ?: return null
        return tasksKeyValue.value as? YAMLMapping
    }

    private fun extractTaskNames(file: YAMLFile): Set<String> {
        return getTasksMapping(file)
            ?.keyValues
            ?.mapNotNull { it.keyText }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            ?: emptySet()
    }

    data class TaskReference(val reference: String, val isAlias: Boolean)

    private fun extractAliasesFromTaskMapping(taskMapping: YAMLMapping): List<String> {
        val result = mutableListOf<String>()

        val alias = taskMapping.keyValues.find { it.keyText == YamlKeys.ALIAS }?.valueText
        if (!alias.isNullOrEmpty()) {
            result.add(alias)
        }

        val aliasesSequence = taskMapping.keyValues.find { it.keyText == YamlKeys.ALIASES }?.value as? YAMLSequence
        aliasesSequence?.items?.mapNotNull { it.value?.text }?.let { result.addAll(it) }

        return result
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

    fun isConditionValueContext(element: PsiElement): Boolean {
        var current: PsiElement? = element
        while (current != null) {
            if (current is YAMLKeyValue && current.keyText == "when") {
                return isTaskWhenContext(current) || isRunItemWhenContext(current)
            }
            current = current.parent
        }
        return false
    }

    private fun isTaskWhenContext(whenKeyValue: YAMLKeyValue): Boolean {
        val taskMapping = whenKeyValue.parent as? YAMLMapping ?: return false
        return isUnderTasksKey(taskMapping)
    }

    private fun isRunItemWhenContext(whenKeyValue: YAMLKeyValue): Boolean {
        val runItemMapping = whenKeyValue.parent as? YAMLMapping ?: return false
        val sequenceItem = runItemMapping.parent as? YAMLSequenceItem ?: return false
        val sequence = sequenceItem.parent ?: return false
        val runKeyValue = sequence.parent as? YAMLKeyValue ?: return false
        if (runKeyValue.keyText != YamlKeys.RUN) return false
        val taskMapping = runKeyValue.parent as? YAMLMapping ?: return false
        return isUnderTasksKey(taskMapping)
    }
}