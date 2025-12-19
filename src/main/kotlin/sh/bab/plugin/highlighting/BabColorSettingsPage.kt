package sh.bab.plugin.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.jetbrains.yaml.YAMLLanguage
import sh.bab.plugin.BabBundle
import sh.bab.plugin.icons.BabIcons
import javax.swing.Icon

private val DESCRIPTORS = arrayOf(
    AttributesDescriptor(
        BabBundle.message("color.settings.structure.section.keyword"),
        BabHighlightingColors.SECTION_KEYWORD
    ),
    AttributesDescriptor(
        BabBundle.message("color.settings.tasks.name"),
        BabHighlightingColors.TASK_NAME
    ),
    AttributesDescriptor(
        BabBundle.message("color.settings.tasks.reference"),
        BabHighlightingColors.TASK_REFERENCE
    ),
    AttributesDescriptor(
        BabBundle.message("color.settings.properties.key"),
        BabHighlightingColors.PROPERTY_KEY
    ),
    AttributesDescriptor(
        BabBundle.message("color.settings.includes.name"),
        BabHighlightingColors.INCLUDE_NAME
    ),
    AttributesDescriptor(
        BabBundle.message("color.settings.variables.interpolation"),
        BabHighlightingColors.VARIABLE_INTERPOLATION
    ),
    AttributesDescriptor(
        BabBundle.message("color.settings.variables.name"),
        BabHighlightingColors.VARIABLE_NAME
    ),
    AttributesDescriptor(
        BabBundle.message("color.settings.env.name"),
        BabHighlightingColors.ENV_VAR_NAME
    ),
    AttributesDescriptor(
        BabBundle.message("color.settings.log.level"),
        BabHighlightingColors.LOG_LEVEL
    ),
    AttributesDescriptor(
        BabBundle.message("color.settings.prompt.type"),
        BabHighlightingColors.PROMPT_TYPE
    )
)

private val TAG_MAP = mapOf(
    "section" to BabHighlightingColors.SECTION_KEYWORD,
    "task" to BabHighlightingColors.TASK_NAME,
    "ref" to BabHighlightingColors.TASK_REFERENCE,
    "prop" to BabHighlightingColors.PROPERTY_KEY,
    "inc" to BabHighlightingColors.INCLUDE_NAME,
    "interp" to BabHighlightingColors.VARIABLE_INTERPOLATION,
    "var" to BabHighlightingColors.VARIABLE_NAME,
    "env" to BabHighlightingColors.ENV_VAR_NAME,
    "level" to BabHighlightingColors.LOG_LEVEL,
    "ptype" to BabHighlightingColors.PROMPT_TYPE
)

private const val DEMO_TEXT = """
<section>vars</section>:
  <var>app_name</var>: myapp
  <var>version</var>: "1.0.0"

<section>env</section>:
  <env>APP_NAME</env>: <interp>${"$"}{{ app_name }}</interp>
  <env>NODE_ENV</env>: production

<section>includes</section>:
  <inc>utils</inc>:
    <prop>babfile</prop>: ./utils/babfile.yml

<section>tasks</section>:
  <task>setup</task>:
    <prop>desc</prop>: Install dependencies
    <prop>run</prop>:
      - <prop>prompt</prop>: confirm_install
        <prop>type</prop>: <ptype>confirm</ptype>
        <prop>message</prop>: Install dependencies?
      - <prop>cmd</prop>: npm install

  <task>build</task>:
    <prop>desc</prop>: Build for production
    <prop>deps</prop>: [<ref>setup</ref>]
    <prop>vars</prop>:
      <var>target</var>: release
    <prop>env</prop>:
      <env>BUILD_TARGET</env>: <interp>${"$"}{{ target }}</interp>
    <prop>run</prop>:
      - <prop>prompt</prop>: env
        <prop>type</prop>: <ptype>select</ptype>
        <prop>message</prop>: Select environment
        <prop>options</prop>: [dev, staging, prod]
      - <prop>log</prop>: Building <interp>${"$"}{{ app_name }}</interp> v<interp>${"$"}{{ version }}</interp>
        <prop>level</prop>: <level>info</level>
      - <prop>task</prop>: <ref>lint</ref>
      - <prop>cmd</prop>: go build -o ./build/<interp>${"$"}{{ app_name }}</interp>
        <prop>platforms</prop>: [linux, darwin]
"""

class BabColorSettingsPage : ColorSettingsPage {

    override fun getIcon(): Icon = BabIcons.FileType

    override fun getHighlighter(): SyntaxHighlighter =
        SyntaxHighlighterFactory.getSyntaxHighlighter(YAMLLanguage.INSTANCE, null, null)!!

    override fun getDemoText(): String = DEMO_TEXT.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = TAG_MAP

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = BabBundle.message("color.settings.display.name")
}
