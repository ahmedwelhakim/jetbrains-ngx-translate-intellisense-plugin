package com.github.ahmedwelhakim.ngxtranslateintellisense.inspection

import com.github.ahmedwelhakim.ngxtranslateintellisense.NgxTranslateIntellisenseBundle
import com.github.ahmedwelhakim.ngxtranslateintellisense.common.NgxTranslateUtils
import com.github.ahmedwelhakim.ngxtranslateintellisense.psi.NgxTranslatePsiUtils
import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateConfigurationStateService
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil

/**
 * Warns when a translation key exists but is not used anywhere in the project code.
 *
 * This annotator scans translation JSON files and identifies keys that don't have
 * any references in the project's TypeScript/JavaScript code, helping maintain
 * a clean and organized translation file without unused entries.
 */
class NgxTranslateUnusedKeyAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val property = element as? JsonProperty ?: return
        val project = property.project
        if (!NgxTranslateUtils.isSupportedProject(project)) return

        // Check if warning annotator is enabled
        if (!NgxTranslateConfigurationStateService.getInstance(project).state.warningUnusedAnnotatorEnabled) return

        val file = property.containingFile.virtualFile ?: return
        if (!NgxTranslateUtils.isTranslationFile(file)) return

        val directoryPath = file.parent?.path ?: return
        val configuredPaths = NgxTranslateConfigurationStateService.getInstance(project).state.i18nPaths
        if (directoryPath !in configuredPaths) return

        // Skip nested properties - only check leaf properties (those with non-object values)
        if (property.value is JsonObject) return

        val fullKey = NgxTranslatePsiUtils.computeFullKey(property)

        // Check if this key is used anywhere in the project
        val isKeyUsed = isTranslationKeyUsed(project, fullKey)

        if (!isKeyUsed) {
            holder.newAnnotation(
                HighlightSeverity.WARNING,
                NgxTranslateIntellisenseBundle.message("translationKeyUnusedWarning", fullKey)
            )
                .range(property.nameElement)
                .create()
        }
    }

    private fun isTranslationKeyUsed(project: Project, key: String): Boolean {
        try {
            val psiManager = PsiManager.getInstance(project)

            // Try to find scope - use allScope or filesScope
            val scope = ProjectScope.getProjectScope(project)


            // Get all TypeScript and JavaScript files in the project
            val tsFiles = FilenameIndex.getAllFilesByExt(project, "ts", scope)
            val jsFiles = FilenameIndex.getAllFilesByExt(project, "js", scope)
            val htmlFiles = FilenameIndex.getAllFilesByExt(project, "html", scope)
            val allFiles = (tsFiles + jsFiles + htmlFiles)


            for (vFile in allFiles) {
                val psiFile = psiManager.findFile(vFile)
                if (psiFile != null && isKeyUsedInFile(psiFile, key)) {
                    return true
                }
            }
        } catch (e: Exception) {
            // If there's any exception during search, be conservative and don't mark as unused
            return true
        }

        return false
    }

    private fun isKeyUsedInFile(psiFile: PsiFile, key: String): Boolean {
        val jsLiterals = PsiTreeUtil.findChildrenOfType(psiFile, JSLiteralExpression::class.java)

        for (literal in jsLiterals) {
            val stringValue = literal.stringValue
            if (stringValue == key) {
                return true
            }
        }

        return false
    }
}






