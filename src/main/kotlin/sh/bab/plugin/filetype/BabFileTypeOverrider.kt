package sh.bab.plugin.filetype

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider
import com.intellij.openapi.vfs.VirtualFile

class BabFileTypeOverrider : FileTypeOverrider {

    override fun getOverriddenFileType(file: VirtualFile): FileType? {
        return if (isBabfile(file)) {
            BabFileType
        } else {
            null
        }
    }
}
