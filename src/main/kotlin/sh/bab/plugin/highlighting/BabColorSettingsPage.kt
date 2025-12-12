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

class BabColorSettingsPage : ColorSettingsPage {

    override fun getIcon(): Icon = BabIcons.FileType

    override fun getHighlighter(): SyntaxHighlighter =
        SyntaxHighlighterFactory.getSyntaxHighlighter(YAMLLanguage.INSTANCE, null, null)!!

    override fun getDemoText(): String = DEMO_TEXT

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = TAG_MAP

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = BabBundle.message("color.settings.display.name")

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor(BabBundle.message("color.settings.section.keyword"), BabHighlightingColors.SECTION_KEYWORD),
            AttributesDescriptor(BabBundle.message("color.settings.task.name"), BabHighlightingColors.TASK_NAME),
            AttributesDescriptor(BabBundle.message("color.settings.property.key"), BabHighlightingColors.PROPERTY_KEY),
            AttributesDescriptor(BabBundle.message("color.settings.task.reference"), BabHighlightingColors.TASK_REFERENCE),
            AttributesDescriptor(BabBundle.message("color.settings.include.name"), BabHighlightingColors.INCLUDE_NAME)
        )

        private val TAG_MAP = mapOf(
            "section" to BabHighlightingColors.SECTION_KEYWORD,
            "task" to BabHighlightingColors.TASK_NAME,
            "prop" to BabHighlightingColors.PROPERTY_KEY,
            "ref" to BabHighlightingColors.TASK_REFERENCE,
            "inc" to BabHighlightingColors.INCLUDE_NAME
        )

        private val DEMO_TEXT = """
<section>env</section>:
  NODE_ENV: production

<section>includes</section>:
  <inc>utils</inc>:
    <prop>babfile</prop>: ./utils/babfile.yml

<section>tasks</section>:
  <task>setup</task>:
    <prop>desc</prop>: Install dependencies
    <prop>run</prop>:
      - <prop>cmd</prop>: npm install

  <task>build</task>:
    <prop>desc</prop>: Build for production
    <prop>deps</prop>: [<ref>setup</ref>]
    <prop>run</prop>:
      - <prop>task</prop>: <ref>lint</ref>
      - <prop>cmd</prop>: npm run build
        <prop>platforms</prop>: [linux, darwin]
""".trimIndent()
    }
}
