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

object YamlKeys {
    const val TASKS = "tasks"
    const val DEPS = "deps"
    const val RUN = "run"
    const val INCLUDES = "includes"
    const val BABFILE = "babfile"
    const val DESC = "desc"
}

object BabPsiUtil {

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
        val taskKeyValue = taskMapping.parent as? YAMLKeyValue ?: return false
        val tasksMapping = taskKeyValue.parent as? YAMLMapping ?: return false
        val tasksKeyValue = tasksMapping.parent as? YAMLKeyValue ?: return false

        return tasksKeyValue.keyText == YamlKeys.TASKS
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
        val parentTaskKeyValue = taskMapping.parent as? YAMLKeyValue ?: return false
        val tasksMapping = parentTaskKeyValue.parent as? YAMLMapping ?: return false
        val tasksKeyValue = tasksMapping.parent as? YAMLKeyValue ?: return false

        return tasksKeyValue.keyText == YamlKeys.TASKS
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

    fun findCurrentTaskName(element: PsiElement): String? {
        var parent: PsiElement? = element.parent
        while (parent != null) {
            if (parent is YAMLKeyValue) {
                val grandparent = parent.parent
                if (grandparent is YAMLMapping) {
                    val greatGrandparent = grandparent.parent
                    if (greatGrandparent is YAMLKeyValue && greatGrandparent.keyText == YamlKeys.TASKS) {
                        return parent.keyText
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
        val filePath = file.virtualFile?.path ?: return emptySet()
        if (filePath in visited) return emptySet()
        visited.add(filePath)

        val result = mutableSetOf<String>()

        for (taskName in extractTaskNames(file)) {
            result.add(if (prefix.isEmpty()) taskName else "$prefix:$taskName")
        }

        for (includeName in extractIncludeNames(file)) {
            val includedFile = getIncludedYamlFile(file, includeName) ?: continue
            val nestedPrefix = if (prefix.isEmpty()) includeName else "$prefix:$includeName"
            result.addAll(extractAllTaskReferences(includedFile, nestedPrefix, visited))
        }

        return result
    }

    fun resolveTaskReference(file: YAMLFile, reference: String, visited: MutableSet<String> = mutableSetOf()): YAMLKeyValue? {
        val filePath = file.virtualFile?.path ?: return null
        if (filePath in visited) return null
        visited.add(filePath)

        getTaskKeyValues(file).find { it.keyText == reference }?.let { return it }

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
}