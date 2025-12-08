package sh.bab.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import sh.bab.plugin.settings.BabSettings

class BabProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val settings = BabSettings.getInstance(project)
        if (settings.babBinaryPath.isEmpty()) {
            BabSettings.detectBabBinary()?.let { detectedPath ->
                settings.babBinaryPath = detectedPath
            }
        }
    }
}
