package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.inlay

import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.NgxTranslateToolsetBundle
import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import javax.swing.JComponent
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class NgxTranslateInlayHintsProvider :
    InlayHintsProvider<NoSettings> {

    override val key = SettingsKey<NoSettings>("ngx.translate.inlay.hints")
    override val name: String = NgxTranslateToolsetBundle.message("inlayHintsProviderName")
    override val previewText: String = NgxTranslateToolsetBundle.message("inlayHintsPreviewText")

    override fun createSettings(): NoSettings = NoSettings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return NgxTranslateInlayHintsCollector(editor)
    }

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent = JPanel()
        }
    }

    override val isVisibleInSettings: Boolean = true
}
