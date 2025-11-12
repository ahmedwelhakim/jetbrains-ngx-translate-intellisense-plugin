package com.github.ahmedwelhakim.ngxtranslateintellisense.settings

import com.github.ahmedwelhakim.ngxtranslateintellisense.NgxTranslateIntellisenseBundle
import com.github.ahmedwelhakim.ngxtranslateintellisense.common.NgxTranslateUtils
import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateConfigurationStateService
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
    private val autoDiscoveryCheckbox = JCheckBox(NgxTranslateIntellisenseBundle.message("enableAutoDiscovery"))
    private val inlayHintsCheckbox = JCheckBox(NgxTranslateIntellisenseBundle.message("enableInlayHints"))
    private val pathsModel = DefaultListModel<String>()
    private val pathsList = JBList(pathsModel)

    private val panel: JPanel

    init {
        val decorator = ToolbarDecorator.createDecorator(pathsList)
            .setAddAction {
                val chooser = TextFieldWithBrowseButton().apply {
                    addBrowseFolderListener(
                        project,
                        FileChooserDescriptorFactory.createSingleFolderDescriptor()
                            .withTitle(NgxTranslateIntellisenseBundle.message("addI18nFolder"))
                            .withDescription(NgxTranslateIntellisenseBundle.message("addI18nFolderDescription"))

                    )

                }
                val dialog = JOptionPane.showConfirmDialog(
                    null,
                    chooser,
                    NgxTranslateIntellisenseBundle.message("addI18nFolder"),
                    JOptionPane.OK_CANCEL_OPTION
                )
                if (dialog == JOptionPane.OK_OPTION) {
                    val path = NgxTranslateUtils.toSystemIndependent(chooser.text)
                    if (path.isNotBlank()
                        && !pathsModel.contains(path)
                        && NgxTranslateUtils.isTranslationDirectoryNotEmpty(path)
                    ) {
                        pathsModel.addElement(path)
                    } else if (path.isBlank()) {
                        JOptionPane.showMessageDialog(
                            null,
                            NgxTranslateIntellisenseBundle.message("addI18nFolderPleaseChooseFolderError"),
                            NgxTranslateIntellisenseBundle.message("addI18nFolder"),
                            JOptionPane.ERROR_MESSAGE
                        )
                    } else if (!NgxTranslateUtils.isTranslationDirectoryNotEmpty(path)) {
                        JOptionPane.showMessageDialog(
                            null,
                            NgxTranslateIntellisenseBundle.message("addI18nFolderError"),
                            NgxTranslateIntellisenseBundle.message("addI18nFolder"),
                            JOptionPane.ERROR_MESSAGE
                        )
                    } else if (pathsModel.contains(path)) {
                        JOptionPane.showMessageDialog(
                            null,
                            NgxTranslateIntellisenseBundle.message("addI18nFolderDuplicateError"),
                            NgxTranslateIntellisenseBundle.message("addI18nFolder"),
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }
            .setRemoveAction { pathsList.selectedValuesList.forEach { pathsModel.removeElement(it) } }

        val listPanel = decorator.createPanel()

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(
                JBLabel(NgxTranslateIntellisenseBundle.message("defaultLanguageLabel")),
                langField,
                1,
                false
            )
            .addLabeledComponent(
                JBLabel(NgxTranslateIntellisenseBundle.message("enableInlayHintsLabel")),
                inlayHintsCheckbox,
                1,
                false
            )
            .addLabeledComponent(
                JBLabel(NgxTranslateIntellisenseBundle.message("inlayHintLength")),
                inlayLengthSpinner,
                1,
                false
            )
            .addLabeledComponent(
                JBLabel(NgxTranslateIntellisenseBundle.message("enableAutoDiscovery")),
                autoDiscoveryCheckbox,
                1,
                false
            )
            .addSeparator()
            .addComponent(JBLabel(NgxTranslateIntellisenseBundle.message("translationFoldersLabel")))
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

    override fun getDisplayName(): String = NgxTranslateIntellisenseBundle.message("ngxTranslateIntellisense")
}
