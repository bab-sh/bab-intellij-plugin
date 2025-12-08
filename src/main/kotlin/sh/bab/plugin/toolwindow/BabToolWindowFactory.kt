package sh.bab.plugin.toolwindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import sh.bab.plugin.services.BabFileService

class BabToolWindowFactory : ToolWindowFactory, DumbAware {

    companion object {
        private val LOG = Logger.getInstance(BabToolWindowFactory::class.java)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = BabToolWindowPanel(project)
        val content = toolWindow.contentManager.factory.createContent(panel, null, false)
        content.setDisposer(panel)
        toolWindow.contentManager.addContent(content)
    }

    override suspend fun isApplicableAsync(project: Project): Boolean {
        return try {
            project.service<BabFileService>().findRootBabfile() != null
        } catch (e: Exception) {
            LOG.debug("Failed to check babfile applicability", e)
            false
        }
    }
}
