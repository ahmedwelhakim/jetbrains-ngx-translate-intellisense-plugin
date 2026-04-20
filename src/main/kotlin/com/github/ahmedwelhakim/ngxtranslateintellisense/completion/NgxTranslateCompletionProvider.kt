package com.github.ahmedwelhakim.ngxtranslateintellisense.completion

import com.github.ahmedwelhakim.ngxtranslateintellisense.common.NgxTranslateUtils
import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateConfigurationStateService
import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateTranslationCache
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

/**
 * Completion provider that generates translation key suggestions for code completion.
 *
 * This class is responsible for providing the actual completion suggestions
 * when a user is typing in a string literal context. It retrieves all available
 * translation keys from the project's translation cache and presents them
 * as completion options with "i18n" type text.
 *
 * The provider respects the project's completion settings (BASIC vs SMART mode).
 */
class NgxTranslateCompletionProvider : CompletionProvider<CompletionParameters>() {
    /**
     * Adds completion suggestions for translation keys.
     *
     * This method is called by IntelliJ's code completion system when the user
     * invokes completion in a string literal context. It retrieves all translation
     * keys from the project's translation cache and adds them to the completion result set.
     *
     * The suggestions are only provided if the completion mode matches the project's settings.
     *
     * @param parameters The completion parameters containing context information
     * @param context The processing context for the completion operation
     * @param result The completion result set to populate with suggestions
     */
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.editor.project ?: return

        // Only provide completions for Angular/Nx projects with ngx-translate
        if (!NgxTranslateUtils.isSupportedProject(project)) return

        // Check if the current completion mode matches the project settings
        val configService = NgxTranslateConfigurationStateService.getInstance(project)
        val addToSmartCompletion = configService.state.addToSmartCompletion
        val isSmartCompletion = parameters.completionType == CompletionType.SMART

        // if Basic make it after pressing (Ctrl+Space) more than one time
        if (!isSmartCompletion && parameters.invocationCount <= 1) return
        // If setting is for SMART completion, only show suggestions when explicitly invoked
        if (isSmartCompletion && !addToSmartCompletion) return

        project.getService(NgxTranslateTranslationCache::class.java).getAllKeys().forEach { key ->
            result.addElement(
                LookupElementBuilder
                    .create(key)
                    .withTypeText("i18n", true)

            )
        }
    }
}
