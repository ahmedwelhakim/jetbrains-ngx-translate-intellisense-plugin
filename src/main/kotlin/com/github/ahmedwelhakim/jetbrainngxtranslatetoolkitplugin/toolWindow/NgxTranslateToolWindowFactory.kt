package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.toolWindow

import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.configuration.ConfigurationStateService
import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.services.MyProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel


class NgxTranslateToolWindowFactory : ToolWindowFactory {

//    init {
//        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
//    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = NgxTranslateToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(project), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class NgxTranslateToolWindow(toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<MyProjectService>()

        fun getContent(project: Project): JPanel {
            val stateService = ConfigurationStateService.getInstance(project)
            val state = stateService.state

            val langField = JBTextField(state.lang)
            val pathField = TextFieldWithBrowseButton().apply {
                text = state.i18nPath ?: ""
                addBrowseFolderListener(

                    project,
                    FileChooserDescriptorFactory.createSingleFolderDescriptor()
                )
            }

            val saveButton = JButton("Save").apply {
                addActionListener {
                    stateService.saveState(langField.text, pathField.text)
                }
            }

            return JPanel(BorderLayout(10, 5)).apply {
                add(
                    JPanel(VerticalFlowLayout(4, 4).apply { horizontalFill = true }).apply {
                        add(JLabel("Language"))
                        add(langField)
                        add(JLabel("Path"))
                        add(pathField)
                        add(saveButton)
                    },
                    BorderLayout.PAGE_START
                )
            }
        }
    }
}
