package sh.bab.plugin.model

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.yaml.psi.YAMLKeyValue

data class BabTask(
    val name: String,
    val description: String?,
    val dependencies: List<String>,
    val alias: String? = null,
    val aliases: List<String> = emptyList(),
    val psiElement: YAMLKeyValue? = null
)

data class BabFile(
    val file: VirtualFile,
    val relativePath: String,
    val tasks: List<BabTask>,
    val includes: List<BabInclude>
)

data class BabInclude(
    val prefix: String,
    val babfilePath: String,
    val resolvedFile: BabFile?
)

data class BabExecutable(
    val path: String,
    val version: String?
) {
    override fun toString(): String = path
}

data class TaskReference(
    val reference: String,
    val isAlias: Boolean
)
