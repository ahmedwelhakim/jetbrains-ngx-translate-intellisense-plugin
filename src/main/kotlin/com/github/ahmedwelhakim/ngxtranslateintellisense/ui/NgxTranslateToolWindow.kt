package com.github.ahmedwelhakim.ngxtranslateintellisense.ui

import com.github.ahmedwelhakim.ngxtranslateintellisense.NgxTranslateIntellisenseBundle
import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateConfigurationStateService
import com.github.ahmedwelhakim.ngxtranslateintellisense.settings.NgxTranslateSettingsConfigurable
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.event.HyperlinkEvent
import javax.swing.tree.DefaultTreeModel

/**
 * Tool window coordinator for translation directories, key warnings, and quick actions.
 */
class NgxTranslateToolWindow {
    private lateinit var tree: Tree
    private lateinit var treeModel: DefaultTreeModel
    private lateinit var project: Project
    private lateinit var warningPanel: JPanel
    private lateinit var warningArea: JEditorPane
    private lateinit var warningSupport: NgxTranslateWarningSupport

    /**
     * Creates and returns the main content panel for the tool window.
     * 
     * This method builds the complete UI including the translation directories tree,
     * settings button, refresh button, and sets up file system listeners for auto-refresh.
     * 
     * @param project The IntelliJ project instance
     * @return The main JPanel containing all tool window components
     */
    fun getContent(project: Project): JPanel {
        this.project = project
        warningSupport = NgxTranslateWarningSupport(project)

        val state = NgxTranslateConfigurationStateService.getInstance(project).state
        treeModel = NgxTranslateTreeModelBuilder.createModel(state.i18nPaths)
        tree = Tree(treeModel)
        NgxTranslateTreeUiConfigurer.configure(tree, project)

        warningArea = createWarningArea()
        warningPanel = createWarningPanel(warningArea)
        refreshWarnings(state.i18nPaths)
        subscribeToVfsChanges(state.i18nPaths)

        return JPanel(BorderLayout(0, 0)).apply {
            add(Splitter(true, 0.5f).apply {

                firstComponent = JBScrollPane(tree)
                secondComponent = warningPanel
            }, BorderLayout.CENTER)
            add(createBottomPanel(), BorderLayout.PAGE_END)
        }
    }

    /**
     * Refreshes the translation directories tree with current project state.
     *
     * This method rebuilds the entire tree structure from the current configuration,
     * ensuring the UI reflects any changes in translation directories or files.
     */
    private fun refreshTree() {
        val state = NgxTranslateConfigurationStateService.getInstance(project).state
        treeModel.setRoot(NgxTranslateTreeModelBuilder.buildRoot(state.i18nPaths))
        treeModel.reload()
        NgxTranslateTreeUiConfigurer.expandAll(tree)
        refreshWarnings(state.i18nPaths)
    }

    private fun refreshWarnings(paths: List<String>) {
        val warningHtml = warningSupport.buildMismatchWarningHtml(paths)
        warningPanel.isVisible = warningHtml.isNotBlank()
        warningArea.text = warningHtml.ifBlank { warningSupport.emptyWarningsHtml() }
        warningArea.caretPosition = 0
    }

    private fun createWarningArea(): JEditorPane {
        return JEditorPane("text/html", "").apply {
            isEditable = false
            isOpaque = false
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            addHyperlinkListener { event ->
                if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                    warningSupport.navigateFromWarningLink(event.description)
                }
            }
        }
    }

    private fun createWarningPanel(content: JEditorPane): JPanel {
        val scrollPane = JBScrollPane(content).apply {
            border = BorderFactory.createEmptyBorder(15, 15, 15, 15)
            preferredSize = Dimension(0, 110)
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        }

        return JPanel(BorderLayout(0, 4)).apply {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            add(
                JLabel(
                    NgxTranslateIntellisenseBundle.message("translationKeyWarningsTitle"),
                    AllIcons.General.Warning,
                    JLabel.LEFT
                ),
                BorderLayout.PAGE_START
            )
            add(scrollPane, BorderLayout.CENTER)
        }
    }

    private fun createBottomPanel(): JPanel {
        val settingsButton = JButton("Settings", AllIcons.General.Settings).apply {
            addActionListener {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, NgxTranslateSettingsConfigurable::class.java)
                ApplicationManager.getApplication().invokeLater { refreshTree() }
            }
        }

        val refreshButton = JButton("Refresh", AllIcons.General.Refresh).apply {
            addActionListener {
                ApplicationManager.getApplication().invokeLater { refreshTree() }
            }
        }

        return JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(settingsButton)
            add(refreshButton)
        }
    }

    private fun subscribeToVfsChanges(configuredPaths: List<String>) {
        project.messageBus.connect().subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: MutableList<out VFileEvent>) {
                    val hasChangedTranslationJson = events.any { event ->
                        event.file?.extension == "json" &&
                                configuredPaths.any { path -> event.file?.path?.startsWith(path) == true }
                    }

                    if (hasChangedTranslationJson) {
                        ApplicationManager.getApplication().invokeLater { refreshTree() }
                    }
                }
            }
        )
    }
}
