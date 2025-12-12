package sh.bab.plugin.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

object BabHighlightingColors {

    @JvmField
    val SECTION_KEYWORD: TextAttributesKey =
        createTextAttributesKey("BAB_SECTION_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)

    @JvmField
    val TASK_NAME: TextAttributesKey =
        createTextAttributesKey("BAB_TASK_NAME", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)

    @JvmField
    val PROPERTY_KEY: TextAttributesKey =
        createTextAttributesKey("BAB_PROPERTY_KEY", DefaultLanguageHighlighterColors.METADATA)

    @JvmField
    val TASK_REFERENCE: TextAttributesKey =
        createTextAttributesKey("BAB_TASK_REFERENCE", DefaultLanguageHighlighterColors.FUNCTION_CALL)

    @JvmField
    val INCLUDE_NAME: TextAttributesKey =
        createTextAttributesKey("BAB_INCLUDE_NAME", DefaultLanguageHighlighterColors.CONSTANT)
}
