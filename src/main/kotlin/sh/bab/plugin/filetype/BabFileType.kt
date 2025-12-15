package sh.bab.plugin.filetype

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.yaml.YAMLLanguage
import sh.bab.plugin.BabBundle
import sh.bab.plugin.icons.BabIcons
import javax.swing.Icon

private val BABFILE_PATTERN = Regex(
    "^babfile(?:\\.[^.]+)?\\.(yml|yaml)$",
    RegexOption.IGNORE_CASE
)

fun isBabfile(file: VirtualFile): Boolean {
    return isBabfileName(file.name)
}

fun isBabfileName(fileName: String): Boolean {
    return BABFILE_PATTERN.matches(fileName)
}

object BabFileType : LanguageFileType(YAMLLanguage.INSTANCE) {

    override fun getName(): String = "Babfile"

    override fun getDescription(): String = BabBundle.message("filetype.description")

    override fun getDefaultExtension(): String = "yml"

    override fun getIcon(): Icon = BabIcons.FileType
}
