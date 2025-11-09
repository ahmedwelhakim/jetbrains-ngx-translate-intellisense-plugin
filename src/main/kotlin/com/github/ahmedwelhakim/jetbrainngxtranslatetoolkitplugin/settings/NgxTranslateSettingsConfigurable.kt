package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.settings

import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.NgxTranslateToolsetBundle
import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.services.NgxTranslateConfigurationStateService
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.*

class NgxTranslateSettingsConfigurable(private val project: Project) : Configurable {
    private val service = NgxTranslateConfigurationStateService.getInstance(project)

    private val langField = JBTextField()
    private val inlayLengthSpinner = JSpinner(SpinnerNumberModel(40, 10, 200, 5))
    private val autoDiscoveryCheckbox = JCheckBox(NgxTranslateToolsetBundle.message("enableAutoDiscovery"))
    private val inlayHintsCheckbox = JCheckBox(NgxTranslateToolsetBundle.message("enableInlayHints"))
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
                    NgxTranslateToolsetBundle.message("addI18nFolder"),
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

        val listPanel = decorator.createPanel()

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel(NgxTranslateToolsetBundle.message("defaultLanguageLabel")), langField, 1, false)
            .addLabeledComponent(JBLabel(NgxTranslateToolsetBundle.message("enableInlayHintsLabel")), inlayHintsCheckbox, 1, false)
            .addLabeledComponent(JBLabel(NgxTranslateToolsetBundle.message("inlayHintLength")), inlayLengthSpinner, 1, false)
            .addLabeledComponent(JBLabel(NgxTranslateToolsetBundle.message("enableAutoDiscovery")), autoDiscoveryCheckbox, 1, false)
            .addSeparator()
            .addComponent(JBLabel(NgxTranslateToolsetBundle.message("translationFoldersLabel")))
            .addComponent(listPanel)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun createComponent(): JComponent = panel

    override fun isModified(): Boolean {
        val state = service.state
        return langField.text != state.lang ||
                (inlayLengthSpinner.value as Int) != state.inlayHintLength ||
                autoDiscoveryCheckbox.isSelected != state.autoDiscoveryEnabled ||
                inlayHintsCheckbox.isSelected != state.inlayHintEnabled ||
                pathsModel.elements().toList() != state.i18nPaths
    }

    override fun apply() {
        service.saveSettings(
            langField.text,
            pathsModel.elements().toList().toMutableList(),
            inlayLengthSpinner.value as Int,
            inlayHintsCheckbox.isSelected,
            autoDiscoveryCheckbox.isSelected
        )
    }

    override fun reset() {
        val state = service.state
        langField.text = state.lang
        inlayLengthSpinner.value = state.inlayHintLength
        autoDiscoveryCheckbox.isSelected = state.autoDiscoveryEnabled
        inlayHintsCheckbox.isSelected = state.inlayHintEnabled
        pathsModel.clear()
        state.i18nPaths.forEach { pathsModel.addElement(it) }
    }

    override fun getDisplayName(): String = NgxTranslateToolsetBundle.message("ngxTranslateToolkit")
}
