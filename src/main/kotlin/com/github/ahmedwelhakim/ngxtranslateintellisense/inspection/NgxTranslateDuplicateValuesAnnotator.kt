package com.github.ahmedwelhakim.ngxtranslateintellisense.inspection

import com.github.ahmedwelhakim.ngxtranslateintellisense.NgxTranslateIntellisenseBundle
import com.github.ahmedwelhakim.ngxtranslateintellisense.common.NgxTranslateUtils
import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateConfigurationStateService
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

/**
 * Warns when a translation value is duplicated within the same translation file.
 *
 * This annotator helps identify potential translation issues where multiple keys
 * have the same value, which might indicate copied text that should be different
 * or could be consolidated into a single entry.
 */
class NgxTranslateDuplicateValuesAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val property = element as? JsonProperty ?: return
        val project = property.project
        if (!NgxTranslateUtils.isSupportedProject(project)) return

        // Check if duplicate values annotator is enabled
        if (!NgxTranslateConfigurationStateService.getInstance(project).state.warningDuplicateValuesAnnotatorEnabled) return

        val file = property.containingFile.virtualFile ?: return
        if (!NgxTranslateUtils.isTranslationFile(file)) return

        val directoryPath = file.parent?.path ?: return
        val configuredPaths = NgxTranslateConfigurationStateService.getInstance(project).state.i18nPaths
        if (directoryPath !in configuredPaths) return

        // Skip nested properties - only check leaf properties (those with non-object values)
        if (property.value is JsonObject) return

        val stringValue = (property.value as? JsonStringLiteral)?.value ?: return

        // Skip empty values
        if (stringValue.isBlank()) return

        // Find all string values in the current file and check for duplicates
        val jsonFile = property.containingFile as? JsonFile ?: return
        val root = jsonFile.topLevelValue as? JsonObject ?: return

        val valueOccurrences = mutableMapOf<String, MutableList<JsonProperty>>()
        collectValueOccurrences(root, valueOccurrences)

        // If this value appears more than once, annotate it as a warning
        val duplicateProperties = valueOccurrences[stringValue] ?: emptyList()
        if (duplicateProperties.size > 1 && property in duplicateProperties) {
            holder.newAnnotation(
                HighlightSeverity.WEAK_WARNING,
                NgxTranslateIntellisenseBundle.message("translationDuplicateValueWarning", stringValue)
            )
                .range(property.value ?: property)
                .create()
        }
    }

    private fun collectValueOccurrences(
        obj: JsonObject,
        valueOccurrences: MutableMap<String, MutableList<JsonProperty>>
    ) {
        for (property in obj.propertyList) {
            when (val value = property.value) {
                // Leaf property with string value
                is JsonStringLiteral -> {
                    val strValue = value.value
                    if (strValue.isNotBlank()) {
                        valueOccurrences.getOrPut(strValue) { mutableListOf() }.add(property)
                    }
                }
                // Nested object - recurse
                is JsonObject -> {
                    collectValueOccurrences(value, valueOccurrences)
                }
            }
        }
    }
}


