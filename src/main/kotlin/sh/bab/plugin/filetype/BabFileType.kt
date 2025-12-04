package sh.bab.plugin.filetype

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.yaml.YAMLLanguage
import sh.bab.plugin.BabBundle
import sh.bab.plugin.icons.BabIcons
import javax.swing.Icon

class BabFileType private constructor() : LanguageFileType(YAMLLanguage.INSTANCE) {

    override fun getName(): String = "Babfile"

    override fun getDescription(): String = BabBundle.message("filetype.description")

    override fun getDefaultExtension(): String = "yml"

    override fun getIcon(): Icon = BabIcons.FileType

    companion object {
        @JvmField
        val INSTANCE = BabFileType()

        fun isBabfile(file: VirtualFile): Boolean {
            val fileName = file.name.lowercase()
            return fileName == "babfile.yml" || fileName == "babfile.yaml"
        }
    }
}
