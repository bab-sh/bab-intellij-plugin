package sh.bab.plugin.toolwindow

import com.intellij.openapi.vfs.VirtualFile
import sh.bab.plugin.services.BabTask

sealed class BabTreeNode {
    abstract val displayName: String
}

data class BabFileNode(
    val file: VirtualFile,
    val relativePath: String,
    val includePrefix: String? = null
) : BabTreeNode() {
    override val displayName: String
        get() = if (includePrefix != null) "$includePrefix ($relativePath)" else relativePath
}

data class BabTaskNode(
    val task: BabTask,
    val parentFile: VirtualFile
) : BabTreeNode() {
    val name: String get() = task.name
    val description: String? get() = task.description
    val psiElement get() = task.psiElement

    override val displayName: String
        get() = name
}
