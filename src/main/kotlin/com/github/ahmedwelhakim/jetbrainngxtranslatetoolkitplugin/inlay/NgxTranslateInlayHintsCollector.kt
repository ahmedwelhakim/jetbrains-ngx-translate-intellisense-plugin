package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.inlay

import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.common.getTruncatedValue
import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.services.NgxTranslateConfigurationStateService
import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.services.NgxTranslateTranslationCache
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement

@Suppress("UnstableApiUsage")
class NgxTranslateInlayHintsCollector(
    editor: Editor
) : FactoryInlayHintsCollector(editor) {

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        val config = element.project.getService(NgxTranslateConfigurationStateService::class.java)
        if (!config.state.inlayHintEnabled) return true
        // Only process string literals
        val literal = element as? JSLiteralExpression ?: return true
        val key = literal.stringValue ?: return true

        val project = element.project
        val cache = project.getService(NgxTranslateTranslationCache::class.java)
        if (!cache.hasKey(key)) return true

        // Fetch the translation value from cache or JSON (we’ll add that next)
        val value = getTruncatedValue(cache.getValueForKey(key), project) ?: return true

        // Add inlay hint after the literal
        val presentation = factory.smallText(value)
        val centered = factory.roundWithBackground(factory.inset(presentation, top = 1, down = 2))
        sink.addInlineElement(
            literal.textRange.endOffset,
            relatesToPrecedingText = true,
            centered,
            false
        )
        return true
    }
}