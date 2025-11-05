package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.settings

import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.services.NgxTranslateConfigurationStateService
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBList
import com.intellij.util.ui.FormBuilder
import javax.swing.*
import javax.swing.SpinnerNumberModel

class NgxTranslateSettingsConfigurable(private val project: Project) : Configurable {
    private val service = NgxTranslateConfigurationStateService.getInstance(project)

    private val langField = JBTextField()
    private val inlayLengthSpinner = JSpinner(SpinnerNumberModel(40, 10, 200, 5))

    private val pathsModel = DefaultListModel<String>()
    private val pathsList = JBList(pathsModel)

    private val panel: JPanel

    init {
        val decorator = ToolbarDecorator.createDecorator(pathsList)
            .setAddAction {
                val chooser = TextFieldWithBrowseButton()
                chooser.addBrowseFolderListener(

                    project,
                    FileChooserDescriptorFactory.createSingleFolderDescriptor()
                )
                val dialog = JOptionPane.showConfirmDialog(
                    null,
                    chooser,
                    "Add i18n Folder",
                    JOptionPane.OK_CANCEL_OPTION
                )
                if (dialog == JOptionPane.OK_OPTION) {
                    val path = chooser.text
                    if (path.isNotBlank() && !pathsModel.contains(path)) {
                        pathsModel.addElement(path)
                    }
                }
            }
            .setRemoveAction { pathsList.selectedValuesList.forEach { pathsModel.removeElement(it) } }
            .setMoveUpAction { moveItem(-1) }
            .setMoveDownAction { moveItem(1) }

        val listPanel = decorator.createPanel()

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Default Language:"), langField, 1, false)
            .addLabeledComponent(JBLabel("Inlay Hint Length:"), inlayLengthSpinner, 1, false)
            .addSeparator()
            .addComponent(JBLabel("Translation Folders (i18n Paths):"))
            .addComponent(listPanel)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun moveItem(direction: Int) {
        val index = pathsList.selectedIndex
        if (index < 0) return
        val newIndex = index + direction
        if (newIndex in 0 until pathsModel.size) {
            val element = pathsModel.remove(index)
            pathsModel.add(newIndex, element)
            pathsList.selectedIndex = newIndex
        }
    }

    override fun createComponent(): JComponent = panel

    override fun isModified(): Boolean {
        val state = service.state
        return langField.text != state.lang ||
                (inlayLengthSpinner.value as Int) != state.inlayHintLength ||
                pathsModel.elements().toList() != state.i18nPaths
    }

    override fun apply() {
        service.saveSettings(
            langField.text,
            pathsModel.elements().toList().toMutableList(),
            inlayLengthSpinner.value as Int
        )
    }

    override fun reset() {
        val state = service.state
        langField.text = state.lang
        inlayLengthSpinner.value = state.inlayHintLength
        pathsModel.clear()
        state.i18nPaths.forEach { pathsModel.addElement(it) }
    }

    override fun getDisplayName(): String = "Ngx Translate Toolkit"

}