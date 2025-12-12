package sh.bab.plugin.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.*
import sh.bab.plugin.filetype.isBabfile
import sh.bab.plugin.util.BabPsiUtil
import sh.bab.plugin.util.YamlKeys

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
        }
    }

    private fun annotateScalar(scalar: YAMLScalar, holder: AnnotationHolder) {
        if (BabPsiUtil.isTaskReferenceContext(scalar)) {
            highlight(scalar, holder, BabHighlightingColors.TASK_REFERENCE)
        }
    }

    private fun highlight(element: PsiElement, holder: AnnotationHolder, key: TextAttributesKey) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element.textRange)
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

    companion object {
        private val ROOT_SECTION_KEYS = setOf(YamlKeys.TASKS, YamlKeys.INCLUDES, "env")
        private val TASK_PROPERTY_KEYS = setOf(YamlKeys.DESC, YamlKeys.RUN, YamlKeys.DEPS, "env", "platforms")
        private val RUN_ITEM_KEYS = setOf("cmd", "task", "env", "platforms")
    }
}
