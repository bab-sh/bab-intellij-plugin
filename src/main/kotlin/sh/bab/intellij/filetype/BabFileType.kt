package sh.bab.intellij.filetype

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.yaml.YAMLLanguage
import sh.bab.intellij.BabBundle
import javax.swing.Icon

class BabFileType private constructor() : LanguageFileType(YAMLLanguage.INSTANCE) {

    override fun getName(): String = "Babfile"

    override fun getDescription(): String = BabBundle.message("filetype.description")

    override fun getDefaultExtension(): String = "yml"

    override fun getIcon(): Icon = AllIcons.FileTypes.Yaml

    companion object {
        @JvmField
        val INSTANCE = BabFileType()
    }
}
