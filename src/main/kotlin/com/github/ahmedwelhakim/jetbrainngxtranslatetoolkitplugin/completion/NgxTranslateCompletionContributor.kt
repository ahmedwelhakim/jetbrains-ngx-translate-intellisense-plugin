package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.lang.javascript.JSTokenTypes
import com.intellij.patterns.PlatformPatterns

class NgxTranslateCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(JSTokenTypes.STRING_LITERAL),
            NgxTranslateCompletionProvider()
        )
    }
}