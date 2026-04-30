package com.github.ahmedwelhakim.ngxtranslateintellisense.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.treeStructure.Tree
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

private const val TRANSLATION_TREE_ROOT = "Translation Directories"

internal data class DirectoryNode(val path: String)
internal data class FileNode(val name: String, val path: String)

internal object NgxTranslateTreeModelBuilder {
    fun createModel(i18nPaths: List<String>): DefaultTreeModel = DefaultTreeModel(buildRoot(i18nPaths))

    fun buildRoot(i18nPaths: List<String>): DefaultMutableTreeNode {
        val root = DefaultMutableTreeNode(TRANSLATION_TREE_ROOT)

        i18nPaths.forEach { path ->
            val dirNode = DefaultMutableTreeNode(DirectoryNode(path))
            root.add(dirNode)

            val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://$path")
            virtualFile?.children?.forEach { file ->
                if (!file.isDirectory && file.extension == "json") {
                    dirNode.add(DefaultMutableTreeNode(FileNode(file.name, file.path)))
                }
            }
        }

        return root
    }
}

internal object NgxTranslateTreeUiConfigurer {
    fun configure(tree: Tree, project: Project) {
        tree.isRootVisible = true
        tree.showsRootHandles = true
        tree.cellRenderer = TranslationTreeCellRenderer()

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount != 1) return

                val path = tree.getPathForLocation(e.x, e.y) ?: return
                val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
                val fileNode = node.userObject as? FileNode ?: return

                val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://${fileNode.path}") ?: return
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
            }
        })

        expandAll(tree)
    }

    fun expandAll(tree: JTree) {
        for (i in 0 until tree.rowCount) {
            tree.expandRow(i)
        }
    }

    private class TranslationTreeCellRenderer : DefaultTreeCellRenderer() {
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
}

