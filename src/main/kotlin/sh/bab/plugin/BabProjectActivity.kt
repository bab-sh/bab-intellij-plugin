package sh.bab.plugin

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import sh.bab.plugin.settings.BabSettings
import sh.bab.plugin.util.BabBinaryUtil

class BabProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val settings = project.service<BabSettings>()
        if (settings.babBinaryPath.isEmpty()) {
            BabBinaryUtil.detectBabBinary()?.let { detectedPath ->
                settings.babBinaryPath = detectedPath
            }
        }
    }
}
