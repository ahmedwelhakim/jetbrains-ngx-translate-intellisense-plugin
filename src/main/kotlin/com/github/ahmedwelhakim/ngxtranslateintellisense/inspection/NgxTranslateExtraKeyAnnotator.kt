package com.github.ahmedwelhakim.ngxtranslateintellisense.inspection

import com.github.ahmedwelhakim.ngxtranslateintellisense.NgxTranslateIntellisenseBundle
import com.github.ahmedwelhakim.ngxtranslateintellisense.common.NgxTranslateUtils
import com.github.ahmedwelhakim.ngxtranslateintellisense.psi.NgxTranslatePsiUtils
import com.github.ahmedwelhakim.ngxtranslateintellisense.psi.NgxTranslatePsiUtils.TranslationKeyMismatchType
import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateConfigurationStateService
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.json.psi.JsonProperty
import com.intellij.psi.PsiElement

/**
 * Warns when a translation key exists only in the current locale file.
 */
class NgxTranslateExtraKeyAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val property = element as? JsonProperty ?: return
        val project = property.project
        if (!NgxTranslateUtils.isSupportedProject(project)) return

        // Check if error annotator is enabled
        if (!NgxTranslateConfigurationStateService.getInstance(project).state.errorAnnotatorEnabled) return

        val file = property.containingFile.virtualFile ?: return
        if (!NgxTranslateUtils.isTranslationFile(file)) return

        val directoryPath = file.parent?.path ?: return
        val configuredPaths = NgxTranslateConfigurationStateService.getInstance(project).state.i18nPaths
        if (directoryPath !in configuredPaths) return

        val fullKey = NgxTranslatePsiUtils.computeFullKey(property)
        val consistency = NgxTranslatePsiUtils.validateTranslationKeysConsistency(project, directoryPath)
        val extraKeys = consistency.mismatchDetails[file.name]
            ?.asSequence()
            ?.filter { it.type == TranslationKeyMismatchType.EXTRA }
            ?.map { it.key }
            ?.toSet()
            ?: emptySet()

        if (fullKey !in extraKeys) return

        holder.newAnnotation(
            HighlightSeverity.ERROR,
            NgxTranslateIntellisenseBundle.message("translationKeyExtraWarning", fullKey)
        )
            .range(property.nameElement)
            .create()
    }
}

