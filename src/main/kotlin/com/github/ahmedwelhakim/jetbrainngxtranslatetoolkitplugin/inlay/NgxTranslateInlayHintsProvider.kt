package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.inlay


import com.intellij.codeInsight.hints.*
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.JComponent
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class NgxTranslateInlayHintsProvider :
    InlayHintsProvider<NoSettings> {

    override val key = SettingsKey<NoSettings>("ngx.translate.inlay.hints")
    override val name: String = "Ngx Translate Inline Values"
    override val previewText: String = "\"HELLO.WORLD\": \"Hello world\""

    override fun createSettings(): NoSettings = NoSettings()

    override fun getCollectorFor(
        file: com.intellij.psi.PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return NgxTranslateInlayHintsCollector(editor)
    }

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener:ChangeListener): JComponent = JPanel()
        }
    }
    override val isVisibleInSettings: Boolean = true
}