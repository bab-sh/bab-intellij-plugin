package sh.bab.plugin.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLSequence
import sh.bab.plugin.filetype.isBabfile

data class BabTask(
    val name: String,
    val description: String?,
    val dependencies: List<String>,
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

@Service(Service.Level.PROJECT)
class BabFileService(private val project: Project) {

    companion object {
        private val LOG = Logger.getInstance(BabFileService::class.java)
    }

    fun findRootBabfile(): VirtualFile? {
        val baseDir = project.basePath?.let { VirtualFileManager.getInstance().findFileByUrl("file://$it") }
            ?: return null

        val babfiles = baseDir.children
            ?.filter { isBabfile(it) }
            ?.sortedBy { it.name }
            ?: return null

        return babfiles.find { it.name.equals("babfile.yml", ignoreCase = true) }
            ?: babfiles.find { it.name.equals("babfile.yaml", ignoreCase = true) }
            ?: babfiles.firstOrNull()
    }

    private fun parseBabfile(file: VirtualFile, basePath: String = "", visited: MutableSet<String> = mutableSetOf()): BabFile? {
        val filePath = file.path
        if (filePath in visited) {
            LOG.debug("Skipping already visited babfile: $filePath")
            return null  // cycle detection
        }
        visited.add(filePath)
        LOG.debug("Parsing babfile: $filePath")

        return ApplicationManager.getApplication().runReadAction<BabFile?> {
            val psiFile = PsiManager.getInstance(project).findFile(file) as? YAMLFile ?: return@runReadAction null
            val rootMapping = psiFile.documents.firstOrNull()?.topLevelValue as? YAMLMapping ?: return@runReadAction null

            val relativePath = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"

            BabFile(
                file = file,
                relativePath = relativePath,
                tasks = parseTasksFromMapping(rootMapping),
                includes = parseIncludesFromMapping(rootMapping, file.parent, visited)
            )
        }
    }

    fun getBabfileTree(): BabFile? {
        val rootFile = findRootBabfile() ?: return null
        return parseBabfile(rootFile)
    }

    private fun parseTasksFromMapping(rootMapping: YAMLMapping): List<BabTask> {
        val tasksKeyValue = rootMapping.keyValues.find { it.keyText == "tasks" } ?: return emptyList()
        val tasksMapping = tasksKeyValue.value as? YAMLMapping ?: return emptyList()

        return tasksMapping.keyValues.mapNotNull { taskKeyValue ->
            val taskName = taskKeyValue.keyText
            if (taskName.isEmpty()) return@mapNotNull null

            val taskMapping = taskKeyValue.value as? YAMLMapping
            val description = taskMapping?.keyValues?.find { it.keyText == "desc" }?.valueText
            val depsSequence = taskMapping?.keyValues?.find { it.keyText == "deps" }?.value as? YAMLSequence
            val dependencies = depsSequence?.items?.mapNotNull { it.value?.text } ?: emptyList()

            BabTask(
                name = taskName,
                description = description,
                dependencies = dependencies,
                psiElement = taskKeyValue
            )
        }
    }

    private fun parseIncludesFromMapping(rootMapping: YAMLMapping, parentDir: VirtualFile?, visited: MutableSet<String>): List<BabInclude> {
        val includesKeyValue = rootMapping.keyValues.find { it.keyText == "includes" } ?: return emptyList()
        val includesMapping = includesKeyValue.value as? YAMLMapping ?: return emptyList()

        return includesMapping.keyValues.mapNotNull { includeKeyValue ->
            val prefix = includeKeyValue.keyText.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val includeMapping = includeKeyValue.value as? YAMLMapping
            val babfilePath = includeMapping?.keyValues?.find { it.keyText == "babfile" }?.valueText ?: return@mapNotNull null

            val resolvedFile = resolveIncludePath(babfilePath, parentDir)?.let { includedFile ->
                parseBabfile(includedFile, prefix, visited)
            }

            BabInclude(prefix = prefix, babfilePath = babfilePath, resolvedFile = resolvedFile)
        }
    }

    private fun resolveIncludePath(path: String, parentDir: VirtualFile?): VirtualFile? {
        if (parentDir == null) return null

        return if (path.startsWith("/")) {
            VirtualFileManager.getInstance().findFileByUrl("file://$path")
        } else {
            parentDir.findFileByRelativePath(path)
        }
    }
}
