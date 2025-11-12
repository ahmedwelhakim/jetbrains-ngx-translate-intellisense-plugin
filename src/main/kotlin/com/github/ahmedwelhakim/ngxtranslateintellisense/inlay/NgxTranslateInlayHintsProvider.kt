package com.github.ahmedwelhakim.ngxtranslateintellisense.inlay

import com.github.ahmedwelhakim.ngxtranslateintellisense.NgxTranslateIntellisenseBundle
import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Inlay hints provider for ngx-translate translation values.
 * 
 * This class provides inlay hints that display translation values directly
 * in the code editor next to translation keys. This helps developers see
 * the actual translation text without having to navigate to the JSON files.
 * 
 * The provider integrates with IntelliJ's inlay hints system and can be
 * configured through the IDE settings. It displays truncated translation
 * values to avoid cluttering the editor.
 */
@Suppress("UnstableApiUsage")
class NgxTranslateInlayHintsProvider :
    InlayHintsProvider<NoSettings> {

    override val key = SettingsKey<NoSettings>("ngx.translate.inlay.hints")
    override val name: String = NgxTranslateIntellisenseBundle.message("inlayHintsProviderName")
    override val previewText: String = NgxTranslateIntellisenseBundle.message("inlayHintsPreviewText")

    override fun createSettings(): NoSettings = NoSettings()

    /**
     * Creates the collector for generating inlay hints.
     * 
     * @param file The PSI file being processed
     * @param editor The editor where hints will be displayed
     * @param settings The provider settings (always NoSettings for this provider)
     * @param sink The sink for collecting inlay hints
     * @return The configured inlay hints collector
     */
    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return NgxTranslateInlayHintsCollector(editor)
    }

    /**
     * Creates a configurable component for the provider settings.
     * 
     * Since this provider doesn't have configurable settings, it returns
     * an empty configurable component.
     * 
     * @param settings The provider settings (always NoSettings)
     * @return An empty immediate configurable
     */
    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent = JPanel()
        }
    }

    override val isVisibleInSettings: Boolean = true
}
