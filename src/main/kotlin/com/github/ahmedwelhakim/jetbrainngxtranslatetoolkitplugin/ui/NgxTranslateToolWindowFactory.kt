package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.ui

import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.NgxTranslateToolsetBundle
import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.services.NgxTranslateConfigurationStateService
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

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = NgxTranslateToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(project), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class NgxTranslateToolWindow(toolWindow: ToolWindow) {


        fun getContent(project: Project): JPanel {
            val stateService = NgxTranslateConfigurationStateService.getInstance(project)
            val state = stateService.state

            val langField = JBTextField(state.lang)
            val pathField = TextFieldWithBrowseButton().apply {

                addBrowseFolderListener(

                    project,
                    FileChooserDescriptorFactory.createMultipleFoldersDescriptor()
                )
            }

            val saveButton = JButton(NgxTranslateToolsetBundle.message("saveButton")).apply {
//                addActionListener {
//                    stateService.saveSettings(langField.text, mutableListOf(pathField.text), 40)
//                }
            }

            return JPanel(BorderLayout(10, 5)).apply {
                add(
                    JPanel(VerticalFlowLayout(4, 4).apply { horizontalFill = true }).apply {
                        add(JLabel(NgxTranslateToolsetBundle.message("languageLabel")))
                        add(langField)
                        add(JLabel(NgxTranslateToolsetBundle.message("translationDirectory")))
                        add(pathField)
                        add(saveButton)
                    },
                    BorderLayout.PAGE_START
                )
            }
        }
    }
}
