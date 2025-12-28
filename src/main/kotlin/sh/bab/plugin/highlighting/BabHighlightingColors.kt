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
    val TASK_REFERENCE: TextAttributesKey =
        createTextAttributesKey("BAB_TASK_REFERENCE", DefaultLanguageHighlighterColors.FUNCTION_CALL)

    @JvmField
    val PROPERTY_KEY: TextAttributesKey =
        createTextAttributesKey("BAB_PROPERTY_KEY", DefaultLanguageHighlighterColors.METADATA)

    @JvmField
    val INCLUDE_NAME: TextAttributesKey =
        createTextAttributesKey("BAB_INCLUDE_NAME", DefaultLanguageHighlighterColors.CONSTANT)

    @JvmField
    val VARIABLE_INTERPOLATION: TextAttributesKey =
        createTextAttributesKey("BAB_VARIABLE_INTERPOLATION", DefaultLanguageHighlighterColors.STRING)

    @JvmField
    val VARIABLE_NAME: TextAttributesKey =
        createTextAttributesKey("BAB_VARIABLE_NAME", DefaultLanguageHighlighterColors.INSTANCE_FIELD)

    @JvmField
    val ENV_VAR_NAME: TextAttributesKey =
        createTextAttributesKey("BAB_ENV_VAR_NAME", DefaultLanguageHighlighterColors.STATIC_FIELD)

    @JvmField
    val LOG_LEVEL: TextAttributesKey =
        createTextAttributesKey("BAB_LOG_LEVEL", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL)

    @JvmField
    val PROMPT_TYPE: TextAttributesKey =
        createTextAttributesKey("BAB_PROMPT_TYPE", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL)

    @JvmField
    val CONDITION_KEYWORD: TextAttributesKey =
        createTextAttributesKey("BAB_CONDITION_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
}
