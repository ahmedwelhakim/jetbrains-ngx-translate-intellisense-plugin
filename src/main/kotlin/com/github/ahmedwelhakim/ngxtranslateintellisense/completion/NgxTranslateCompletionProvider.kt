package com.github.ahmedwelhakim.ngxtranslateintellisense.completion

import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateTranslationCache
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class NgxTranslateCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.editor.project ?: return
        project.getService(NgxTranslateTranslationCache::class.java).getAllKeys().forEach { key ->
            result.addElement(
                LookupElementBuilder
                    .create(key)
                    .withTypeText("i18n", true)
//                    .withIcon(AllIcons.Nodes.ResourceBundle)
            )
        }
    }
}
