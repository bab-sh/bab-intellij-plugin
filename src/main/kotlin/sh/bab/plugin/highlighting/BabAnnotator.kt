package sh.bab.plugin.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.*
import sh.bab.plugin.filetype.isBabfile
import sh.bab.plugin.util.BabPsiUtil
import sh.bab.plugin.util.YamlKeys

private val ROOT_SECTION_KEYS = setOf(YamlKeys.TASKS, YamlKeys.INCLUDES, "env", "vars", "silent", "output", "dir")
private val TASK_PROPERTY_KEYS = setOf(YamlKeys.DESC, YamlKeys.RUN, YamlKeys.DEPS, "env", "vars", "platforms", "silent", "output", "dir")
private val RUN_ITEM_KEYS = setOf(
    "cmd", "task", "log", "level", "env", "platforms", "silent", "output", "dir",
    "prompt", "type", "message", "default", "defaults", "options",
    "placeholder", "validate", "min", "max", "confirm"
)
private val LOG_LEVELS = setOf("debug", "info", "warn", "error")
private val PROMPT_TYPES = setOf("confirm", "input", "select", "multiselect", "password", "number")
private val INTERPOLATION_PATTERN = Regex("""\$\{\{\s*([^}]+)\s*}}""")

class BabAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile?.virtualFile ?: return
        if (!isBabfile(file)) return

        when (element) {
            is YAMLKeyValue -> annotateKeyValue(element, holder)
            is YAMLScalar -> annotateScalar(element, holder)
        }
    }

    private fun annotateKeyValue(keyValue: YAMLKeyValue, holder: AnnotationHolder) {
        val key = keyValue.key ?: return
        val keyText = keyValue.keyText

        when {
            isRootLevelKey(keyValue) && keyText in ROOT_SECTION_KEYS ->
                highlight(key, holder, BabHighlightingColors.SECTION_KEYWORD)

            isTaskNameKey(keyValue) ->
                highlight(key, holder, BabHighlightingColors.TASK_NAME)

            isTaskPropertyKey(keyValue) && keyText in TASK_PROPERTY_KEYS ->
                highlight(key, holder, BabHighlightingColors.PROPERTY_KEY)

            isRunItemPropertyKey(keyValue) && keyText in RUN_ITEM_KEYS ->
                highlight(key, holder, BabHighlightingColors.PROPERTY_KEY)

            isIncludeNameKey(keyValue) ->
                highlight(key, holder, BabHighlightingColors.INCLUDE_NAME)

            isIncludePropertyKey(keyValue) && keyText == YamlKeys.BABFILE ->
                highlight(key, holder, BabHighlightingColors.PROPERTY_KEY)

            isVariableNameKey(keyValue) ->
                highlight(key, holder, BabHighlightingColors.VARIABLE_NAME)

            isEnvVarNameKey(keyValue) ->
                highlight(key, holder, BabHighlightingColors.ENV_VAR_NAME)
        }

        if (isLogLevelKey(keyValue)) {
            val value = keyValue.value
            if (value is YAMLScalar && value.textValue.lowercase() in LOG_LEVELS) {
                highlight(value, holder, BabHighlightingColors.LOG_LEVEL)
            }
        }

        if (isPromptTypeKey(keyValue)) {
            val value = keyValue.value
            if (value is YAMLScalar && value.textValue.lowercase() in PROMPT_TYPES) {
                highlight(value, holder, BabHighlightingColors.PROMPT_TYPE)
            }
        }
    }

    private fun annotateScalar(scalar: YAMLScalar, holder: AnnotationHolder) {
        if (BabPsiUtil.isTaskReferenceContext(scalar)) {
            highlight(scalar, holder, BabHighlightingColors.TASK_REFERENCE)
        }

        val text = scalar.text
        val startOffset = scalar.textRange.startOffset
        INTERPOLATION_PATTERN.findAll(text).forEach { match ->
            val range = TextRange(startOffset + match.range.first, startOffset + match.range.last + 1)
            highlightRange(range, holder, BabHighlightingColors.VARIABLE_INTERPOLATION)
        }
    }

    private fun highlight(element: PsiElement, holder: AnnotationHolder, key: TextAttributesKey) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element.textRange)
            .textAttributes(key)
            .create()
    }

    private fun highlightRange(range: TextRange, holder: AnnotationHolder, key: TextAttributesKey) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range)
            .textAttributes(key)
            .create()
    }

    private fun isRootLevelKey(keyValue: YAMLKeyValue): Boolean {
        val parent = keyValue.parent as? YAMLMapping ?: return false
        return parent.parent is YAMLDocument
    }

    private fun isTaskNameKey(keyValue: YAMLKeyValue): Boolean {
        val parent = keyValue.parent as? YAMLMapping ?: return false
        val grandparent = parent.parent as? YAMLKeyValue ?: return false
        return grandparent.keyText == YamlKeys.TASKS && isRootLevelKey(grandparent)
    }

    private fun isTaskPropertyKey(keyValue: YAMLKeyValue): Boolean {
        val parent = keyValue.parent as? YAMLMapping ?: return false
        val grandparent = parent.parent as? YAMLKeyValue ?: return false
        return isTaskNameKey(grandparent)
    }

    private fun isRunItemPropertyKey(keyValue: YAMLKeyValue): Boolean {
        val parent = keyValue.parent as? YAMLMapping ?: return false
        val sequenceItem = parent.parent as? YAMLSequenceItem ?: return false
        val runKeyValue = sequenceItem.parent?.parent as? YAMLKeyValue ?: return false
        return runKeyValue.keyText == YamlKeys.RUN && isTaskPropertyKey(runKeyValue)
    }

    private fun isIncludeNameKey(keyValue: YAMLKeyValue): Boolean {
        val parent = keyValue.parent as? YAMLMapping ?: return false
        val grandparent = parent.parent as? YAMLKeyValue ?: return false
        return grandparent.keyText == YamlKeys.INCLUDES && isRootLevelKey(grandparent)
    }

    private fun isIncludePropertyKey(keyValue: YAMLKeyValue): Boolean {
        val parent = keyValue.parent as? YAMLMapping ?: return false
        val grandparent = parent.parent as? YAMLKeyValue ?: return false
        return isIncludeNameKey(grandparent)
    }

    private fun isVariableNameKey(keyValue: YAMLKeyValue): Boolean {
        val parent = keyValue.parent as? YAMLMapping ?: return false
        val grandparent = parent.parent as? YAMLKeyValue ?: return false

        return grandparent.keyText == "vars" &&
            (isRootLevelKey(grandparent) || isTaskPropertyKey(grandparent))
    }

    private fun isEnvVarNameKey(keyValue: YAMLKeyValue): Boolean {
        val parent = keyValue.parent as? YAMLMapping ?: return false
        val grandparent = parent.parent as? YAMLKeyValue ?: return false

        return grandparent.keyText == "env" &&
            (isRootLevelKey(grandparent) || isTaskPropertyKey(grandparent) || isRunItemPropertyKey(grandparent))
    }

    private fun isLogLevelKey(keyValue: YAMLKeyValue): Boolean {
        return keyValue.keyText == "level" && isRunItemPropertyKey(keyValue)
    }

    private fun isPromptTypeKey(keyValue: YAMLKeyValue): Boolean {
        return keyValue.keyText == "type" && isRunItemPropertyKey(keyValue)
    }
}
