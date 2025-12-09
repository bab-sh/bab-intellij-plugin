package sh.bab.plugin.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object BabIcons {
    @JvmField
    val FileType: Icon = IconLoader.getIcon("/icons/bab.svg", BabIcons::class.java)

    @JvmField
    val Task: Icon = IconLoader.getIcon("/icons/task.svg", BabIcons::class.java)
}
