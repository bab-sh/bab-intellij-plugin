package sh.bab.plugin.toolwindow

import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import sh.bab.plugin.icons.BabIcons
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class BabTreeCellRenderer : ColoredTreeCellRenderer() {

    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        val node = (value as? DefaultMutableTreeNode)?.userObject ?: return

        when (node) {
            is BabFileNode -> renderFileNode(node)
            is BabTaskNode -> renderTaskNode(node)
            is String -> {
                icon = BabIcons.FileType
                append(node, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            }
        }
    }

    private fun renderFileNode(node: BabFileNode) {
        icon = BabIcons.FileType
        if (node.includePrefix != null) {
            append(node.includePrefix, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            append(" (${node.relativePath})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        } else {
            append(node.relativePath, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }
    }

    private fun renderTaskNode(node: BabTaskNode) {
        icon = BabIcons.Task
        append(node.name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        node.description?.let { desc ->
            append(" - $desc", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
    }
}
