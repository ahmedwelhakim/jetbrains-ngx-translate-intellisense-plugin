package com.github.ahmedwelhakim.ngxtranslateintellisense.ui

import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateConfigurationStateService
import com.github.ahmedwelhakim.ngxtranslateintellisense.settings.NgxTranslateSettingsConfigurable
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

class NgxTranslateToolWindow() {
    private lateinit var tree: Tree
    private lateinit var treeModel: DefaultTreeModel
    private lateinit var project: Project

    fun getContent(project: Project): JPanel {
        this.project = project
        val stateService = NgxTranslateConfigurationStateService.getInstance(project)
        val state = stateService.state

        // Create tree for translation directories
        tree = createTranslationDirectoriesTree(project, state.i18nPaths)
        val treeScrollPane = JBScrollPane(tree)

        // Listen for file system changes to auto-refresh
        project.messageBus.connect().subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: MutableList<out VFileEvent>) {
                    // Refresh if any JSON files changed in translation directories
                    if (events.any { event ->
                            event.file?.extension == "json" &&
                                    state.i18nPaths.any { path -> event.file?.path?.startsWith(path) == true }
                        }) {
                        ApplicationManager.getApplication().invokeLater {
                            refreshTree()
                        }
                    }
                }
            }
        )


        // Create settings button
        val settingsButton = JButton("Settings", AllIcons.General.Settings).apply {
            addActionListener {
                val result = ShowSettingsUtil.getInstance()
                    .showSettingsDialog(project, NgxTranslateSettingsConfigurable::class.java)
                // Refresh tree after settings dialog closes
                ApplicationManager.getApplication().invokeLater {
                    refreshTree()
                }
            }
        }

        val refreshButton = JButton("Refresh", AllIcons.General.Refresh).apply {
            addActionListener {
                ApplicationManager.getApplication().invokeLater {
                    refreshTree()
                }
            }
        }

        val bottomPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(settingsButton)
            add(refreshButton)
        }

        return JPanel(BorderLayout(10, 5)).apply {
            add(treeScrollPane, BorderLayout.CENTER)
            add(bottomPanel, BorderLayout.PAGE_END)
        }
    }

    private fun refreshTree() {
        val stateService = NgxTranslateConfigurationStateService.getInstance(project)
        val state = stateService.state

        // Clear and rebuild the tree
        val root = DefaultMutableTreeNode("Translation Directories")

        state.i18nPaths.forEach { path ->
            val dirNode = DefaultMutableTreeNode(DirectoryNode(path))
            root.add(dirNode)

            // Add files in the directory
            val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://$path")
            virtualFile?.children?.forEach { file ->
                if (!file.isDirectory && file.extension == "json") {
                    val fileNode = DefaultMutableTreeNode(FileNode(file.name, file.path))
                    dirNode.add(fileNode)
                }
            }
        }

        treeModel.setRoot(root)
        treeModel.reload()

        // Expand all nodes
        for (i in 0 until tree.rowCount) {
            tree.expandRow(i)
        }
    }

    private fun createTranslationDirectoriesTree(project: Project, i18nPaths: List<String>): Tree {
        val root = DefaultMutableTreeNode("Translation Directories")
        treeModel = DefaultTreeModel(root)

        // Add each translation directory and its contents
        i18nPaths.forEach { path ->
            val dirNode = DefaultMutableTreeNode(DirectoryNode(path))
            root.add(dirNode)

            // Add files in the directory
            val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://$path")
            virtualFile?.children?.forEach { file ->
                if (!file.isDirectory && file.extension == "json") {
                    val fileNode = DefaultMutableTreeNode(FileNode(file.name, file.path))
                    dirNode.add(fileNode)
                }
            }
        }

        val tree = Tree(treeModel)
        tree.isRootVisible = true
        tree.showsRootHandles = true

        // Custom cell renderer for icons
        tree.cellRenderer = object : DefaultTreeCellRenderer() {
            override fun getTreeCellRendererComponent(
                tree: JTree?,
                value: Any?,
                sel: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean
            ): java.awt.Component {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)

                if (value is DefaultMutableTreeNode) {
                    when (val userObject = value.userObject) {
                        is DirectoryNode -> {
                            icon = AllIcons.Nodes.Folder
                            val segments = userObject.path.split("/").filter { it.isNotBlank() }
                            text = segments.takeLast(3).joinToString("/")
                            toolTipText = userObject.path
                        }

                        is FileNode -> {
                            icon = AllIcons.FileTypes.Json
                            text = userObject.name
                        }

                        else -> {
                            icon = AllIcons.Nodes.ModuleGroup
                        }
                    }
                }

                return this
            }
        }

        // Expand all nodes by default
        for (i in 0 until tree.rowCount) {
            tree.expandRow(i)
        }

        // Add mouse listener to open files on click
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 1) { // Single-click
                    val path = tree.getPathForLocation(e.x, e.y) ?: return
                    val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return

                    when (val userObject = node.userObject) {
                        is FileNode -> {
                            // Open the file in the editor
                            val virtualFile =
                                VirtualFileManager.getInstance().findFileByUrl("file://${userObject.path}")
                            virtualFile?.let {
                                FileEditorManager.getInstance(project).openFile(it, true)
                            }
                        }
                    }
                }
            }
        })

        return tree
    }

    private data class DirectoryNode(val path: String)
    private data class FileNode(val name: String, val path: String)
}
