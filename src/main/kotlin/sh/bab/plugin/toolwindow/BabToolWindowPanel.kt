package sh.bab.plugin.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.PopupHandler
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.tree.TreeUtil
import sh.bab.plugin.BabBundle
import sh.bab.plugin.services.BabFile
import sh.bab.plugin.services.BabFileService
import sh.bab.plugin.services.BabTask
import java.awt.Component
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class BabToolWindowPanel(
    private val project: Project
) : SimpleToolWindowPanel(true, true), Disposable {

    private val tree: Tree
    private val treeModel: DefaultTreeModel
    private val rootNode: DefaultMutableTreeNode = DefaultMutableTreeNode("Bab Tasks")
    private var isDisposed = false

    companion object {
        private val LOG = Logger.getInstance(BabToolWindowPanel::class.java)
    }

    init {
        treeModel = DefaultTreeModel(rootNode)
        tree = createTree()

        setContent(JBScrollPane(tree))
        toolbar = createToolbar().component

        refresh()
    }

    private fun createTree(): Tree {
        val tree = Tree(treeModel)
        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.cellRenderer = BabTreeCellRenderer()

        TreeSpeedSearch.installOn(tree, false) { path ->
            val node = path.lastPathComponent as? DefaultMutableTreeNode
            when (val userObject = node?.userObject) {
                is BabTreeNode -> userObject.displayName
                is String -> userObject
                else -> ""
            }
        }

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    navigateToSelectedTask()
                }
            }
        })

        tree.addMouseListener(object : PopupHandler() {
            override fun invokePopup(comp: Component, x: Int, y: Int) {
                val actionGroup = createContextMenu()
                val popupMenu = ActionManager.getInstance()
                    .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, actionGroup)
                popupMenu.component.show(comp, x, y)
            }
        })

        return tree
    }

    private fun createToolbar(): ActionToolbar {
        val actionGroup = DefaultActionGroup().apply {
            add(RefreshAction())
            add(CollapseAllAction())
        }
        val toolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.TOOLWINDOW_CONTENT, actionGroup, true)
        toolbar.targetComponent = this
        return toolbar
    }

    private fun createContextMenu(): ActionGroup {
        return DefaultActionGroup().apply {
            add(NavigateToSourceAction())
            addSeparator()
            add(CopyTaskNameAction())
        }
    }

    fun refresh() {
        if (project.isDisposed || isDisposed) return

        AppExecutorUtil.getAppExecutorService().execute {
            if (project.isDisposed || isDisposed) return@execute

            try {
                val babFileService = project.service<BabFileService>()
                val babfileTree = babFileService.getBabfileTree()

                ApplicationManager.getApplication().invokeLater {
                    if (project.isDisposed || isDisposed) return@invokeLater

                    rootNode.removeAllChildren()
                    if (babfileTree != null) {
                        addBabfileToTree(rootNode, babfileTree)
                    }
                    treeModel.reload()
                    TreeUtil.expandAll(tree)
                }
            } catch (e: Exception) {
                LOG.warn("Failed to refresh babfile tree", e)
            }
        }
    }

    override fun dispose() {
        isDisposed = true
    }

    private fun addBabfileToTree(
        parentNode: DefaultMutableTreeNode,
        babFile: BabFile,
        includePrefix: String? = null
    ) {
        val fileNode = BabFileNode(
            file = babFile.file,
            relativePath = babFile.relativePath,
            includePrefix = includePrefix
        )
        val treeNode = DefaultMutableTreeNode(fileNode)

        addTasksHierarchically(treeNode, babFile.tasks, babFile.file)

        babFile.includes.forEach { include ->
            include.resolvedFile?.let { resolvedFile ->
                addBabfileToTree(treeNode, resolvedFile, includePrefix = include.prefix)
            }
        }

        parentNode.add(treeNode)
    }

    private fun addTasksHierarchically(
        parentNode: DefaultMutableTreeNode,
        tasks: List<BabTask>,
        parentFile: com.intellij.openapi.vfs.VirtualFile
    ) {
        val nodeMap = mutableMapOf<String, DefaultMutableTreeNode>()
        val sortedTasks = tasks.sortedBy { it.name }

        for (task in sortedTasks) {
            val parts = task.name.split(":")

            if (parts.size == 1) {
                val existingNode = nodeMap[task.name]
                if (existingNode != null) {
                    val groupNode = existingNode.userObject as? BabTaskGroupNode
                    if (groupNode != null) {
                        existingNode.userObject = BabTaskGroupNode(
                            name = groupNode.name,
                            task = task,
                            parentFile = parentFile
                        )
                    }
                } else {
                    val taskNode = BabTaskNode(task = task, parentFile = parentFile)
                    val treeNode = DefaultMutableTreeNode(taskNode)
                    nodeMap[task.name] = treeNode
                    parentNode.add(treeNode)
                }
            } else {
                var currentParent = parentNode
                var currentPath = ""

                for (i in 0 until parts.size - 1) {
                    val part = parts[i]
                    currentPath = if (currentPath.isEmpty()) part else "$currentPath:$part"

                    var intermediateNode = nodeMap[currentPath]
                    if (intermediateNode == null) {
                        val groupNode = BabTaskGroupNode(
                            name = part,
                            task = null,
                            parentFile = parentFile
                        )
                        intermediateNode = DefaultMutableTreeNode(groupNode)
                        nodeMap[currentPath] = intermediateNode
                        currentParent.add(intermediateNode)
                    }
                    currentParent = intermediateNode
                }

                val leafName = parts.last()
                val taskNode = BabTaskNode(task = task, parentFile = parentFile, leafName = leafName)
                val treeNode = DefaultMutableTreeNode(taskNode)
                currentParent.add(treeNode)
            }
        }
    }

    private fun getSelectedTaskNode(): BabTaskNode? {
        val selectedNode = tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode
        return selectedNode?.userObject as? BabTaskNode
    }

    private fun getSelectedTaskGroupNode(): BabTaskGroupNode? {
        val selectedNode = tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode
        return selectedNode?.userObject as? BabTaskGroupNode
    }

    private fun canNavigateToSelected(): Boolean {
        val taskNode = getSelectedTaskNode()
        if (taskNode?.psiElement != null) return true
        val groupNode = getSelectedTaskGroupNode()
        return groupNode?.psiElement != null
    }

    private fun navigateToSelectedTask() {
        val taskNode = getSelectedTaskNode()
        val groupNode = getSelectedTaskGroupNode()

        val (psiElement, parentFile) = when {
            taskNode?.psiElement != null -> taskNode.psiElement to taskNode.parentFile
            groupNode?.psiElement != null -> groupNode.psiElement to groupNode.parentFile
            else -> return
        }

        val offset = psiElement!!.textOffset
        val descriptor = OpenFileDescriptor(project, parentFile, offset)
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
    }

    private fun getSelectedTaskName(): String? {
        val taskNode = getSelectedTaskNode()
        if (taskNode != null) return taskNode.name
        val groupNode = getSelectedTaskGroupNode()
        return groupNode?.task?.name
    }


    private inner class RefreshAction : AnAction(
        BabBundle.message("toolwindow.action.refresh"),
        BabBundle.message("toolwindow.action.refresh.description"),
        AllIcons.Actions.Refresh
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            refresh()
        }

        override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    private inner class CollapseAllAction : AnAction(
        BabBundle.message("toolwindow.action.collapse"),
        BabBundle.message("toolwindow.action.collapse.description"),
        AllIcons.Actions.Collapseall
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            TreeUtil.collapseAll(tree, 1)
        }

        override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    private inner class NavigateToSourceAction : AnAction(
        BabBundle.message("toolwindow.action.navigate"),
        BabBundle.message("toolwindow.action.navigate.description"),
        AllIcons.Actions.EditSource
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            navigateToSelectedTask()
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = canNavigateToSelected()
        }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class CopyTaskNameAction : AnAction(
        BabBundle.message("toolwindow.action.copy"),
        BabBundle.message("toolwindow.action.copy.description"),
        AllIcons.Actions.Copy
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            val name = getSelectedTaskName() ?: return
            CopyPasteManager.getInstance().setContents(StringSelection(name))
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = getSelectedTaskName() != null
        }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }
}
