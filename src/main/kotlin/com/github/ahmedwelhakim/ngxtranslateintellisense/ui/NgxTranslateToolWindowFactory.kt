package com.github.ahmedwelhakim.ngxtranslateintellisense.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory class for creating the NgxTranslate tool window in the IntelliJ IDE.
 * 
 * This factory is responsible for creating and initializing the tool window
 * that displays translation files and provides access to ngx-translate functionality.
 * It implements the ToolWindowFactory interface to integrate with IntelliJ's
 * tool window system.
 */
class NgxTranslateToolWindowFactory : ToolWindowFactory {

    /**
     * Creates the content for the NgxTranslate tool window.
     * 
     * This method is called by IntelliJ when the tool window is first created.
     * It instantiates the NgxTranslateToolWindow component and adds it to the
     * tool window's content manager.
     * 
     * @param project The current IntelliJ project
     * @param toolWindow The tool window instance to populate with content
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = NgxTranslateToolWindow()
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(project), null, false)
        toolWindow.contentManager.addContent(content)
    }

    /**
     * Determines whether the tool window should be available for the given project.
     * 
     * @param project The project to check availability for
     * @return true if the tool window should be available, false otherwise
     */
    override fun shouldBeAvailable(project: Project) = true
}
