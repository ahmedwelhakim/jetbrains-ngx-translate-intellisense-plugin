package com.github.ahmedwelhakim.ngxtranslateintellisense.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.lang.javascript.JSTokenTypes
import com.intellij.patterns.PlatformPatterns

/**
 * Completion contributor that provides code completion for ngx-translate translation keys.
 * 
 * This class registers completion providers for JavaScript string literals,
 * enabling intelligent code completion when typing translation keys in the code.
 * It integrates with IntelliJ's code completion system to suggest available
 * translation keys from the project's translation files.
 */
class NgxTranslateCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(JSTokenTypes.STRING_LITERAL),
            NgxTranslateCompletionProvider()
        )
    }
}
