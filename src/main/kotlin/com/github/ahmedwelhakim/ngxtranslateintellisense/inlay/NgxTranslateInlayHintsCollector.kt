package com.github.ahmedwelhakim.ngxtranslateintellisense.inlay

import com.github.ahmedwelhakim.ngxtranslateintellisense.common.NgxTranslateUtils
import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateConfigurationStateService
import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateTranslationCache
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement

/**
 * Collector that generates inlay hints for ngx-translate translation keys.
 * 
 * This class is responsible for finding translation keys in the code and
 * creating inlay hints that display the corresponding translation values.
 * It processes JavaScript string literals and checks if they match known
 * translation keys, then displays the translation text as an inline hint.
 * 
 * The hints are displayed with a rounded background and are truncated
 * according to the configured maximum length to maintain readability.
 */
@Suppress("UnstableApiUsage")
class NgxTranslateInlayHintsCollector(
    editor: Editor
) : FactoryInlayHintsCollector(editor) {

    /**
     * Collects inlay hints for the given PSI element.
     * 
     * This method processes each element in the file and creates inlay hints
     * for JavaScript string literals that represent translation keys. The hints
     * are displayed with the actual translation text, truncated to the configured
     * maximum length.
     * 
     * @param element The PSI element to process
     * @param editor The editor where hints will be displayed
     * @param sink The sink for collecting inlay hints
     * @return true to continue processing, false to stop
     */
    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        val config = element.project.getService(NgxTranslateConfigurationStateService::class.java)
        if (!config.state.inlayHintEnabled) return true
        // Only process string literals
        val literal = element as? JSLiteralExpression ?: return true
        val key = literal.stringValue ?: return true

        val project = element.project
        val cache = project.getService(NgxTranslateTranslationCache::class.java)
        if (!cache.hasKey(key)) return true

        // Fetch the translation value from cache or JSON (we'll add that next)
        val value = NgxTranslateUtils.getTruncatedValue(cache.getValueForKey(key), project) ?: return true

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
