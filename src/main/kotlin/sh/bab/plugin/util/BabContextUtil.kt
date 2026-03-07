package sh.bab.plugin.util

import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLSequenceItem
import sh.bab.plugin.model.YamlKeys

object BabContextUtil {

    fun isUnderTasksKey(mapping: YAMLMapping): Boolean {
        val taskKeyValue = mapping.parent as? YAMLKeyValue ?: return false
        val tasksMapping = taskKeyValue.parent as? YAMLMapping ?: return false
        val tasksKeyValue = tasksMapping.parent as? YAMLKeyValue ?: return false
        return tasksKeyValue.keyText == YamlKeys.TASKS
    }

    fun isInsideRunSequence(mapping: YAMLMapping): Boolean {
        val sequenceItem = mapping.parent as? YAMLSequenceItem ?: return false
        val sequence = sequenceItem.parent ?: return false
        val runKeyValue = sequence.parent as? YAMLKeyValue ?: return false
        if (runKeyValue.keyText != YamlKeys.RUN) return false

        val taskMapping = runKeyValue.parent as? YAMLMapping ?: return false
        return isUnderTasksKey(taskMapping)
    }

    fun isInsideParallelBlock(mapping: YAMLMapping): Boolean {
        val sequenceItem = mapping.parent as? YAMLSequenceItem ?: return false
        val sequence = sequenceItem.parent ?: return false
        val parallelKeyValue = sequence.parent as? YAMLKeyValue ?: return false
        if (parallelKeyValue.keyText != YamlKeys.PARALLEL) return false

        val parallelBlockMapping = parallelKeyValue.parent as? YAMLMapping ?: return false
        return isInsideRunSequence(parallelBlockMapping)
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
        return isInsideRunSequence(mapping) || isInsideParallelBlock(mapping)
    }

    private fun checkIncludeHierarchy(babfileKeyValue: YAMLKeyValue): Boolean {
        val includeMapping = babfileKeyValue.parent as? YAMLMapping ?: return false
        val includeKeyValue = includeMapping.parent as? YAMLKeyValue ?: return false
        val includesMapping = includeKeyValue.parent as? YAMLMapping ?: return false
        val includesKeyValue = includesMapping.parent as? YAMLKeyValue ?: return false

        return includesKeyValue.keyText == YamlKeys.INCLUDES
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
        return isInsideRunSequence(runItemMapping) || isInsideParallelBlock(runItemMapping)
    }

    private fun isTaskWhenContext(whenKeyValue: YAMLKeyValue): Boolean {
        val taskMapping = whenKeyValue.parent as? YAMLMapping ?: return false
        return isUnderTasksKey(taskMapping)
    }

    private fun isRunItemWhenContext(whenKeyValue: YAMLKeyValue): Boolean {
        val runItemMapping = whenKeyValue.parent as? YAMLMapping ?: return false
        return isInsideRunSequence(runItemMapping) || isInsideParallelBlock(runItemMapping)
    }
}
