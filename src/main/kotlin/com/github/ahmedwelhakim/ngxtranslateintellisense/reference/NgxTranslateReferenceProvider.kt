package com.github.ahmedwelhakim.ngxtranslateintellisense.reference

import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateTranslationCache
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext

/**
 * Reference provider that creates references for ngx-translate translation keys.
 * 
 * This class is responsible for determining whether a JavaScript string literal
 * should be treated as a translation key reference and creating the appropriate
 * PsiReference objects for navigation and code intelligence features.
 * 
 * The provider checks if the string literal matches any known translation keys
 * and creates references only for valid translation keys.
 */
class NgxTranslateReferenceProvider : PsiReferenceProvider() {
    /**
     * Creates references for JavaScript string literals that represent translation keys.
     * 
     * This method examines each JavaScript literal expression and determines if it
     * represents a valid translation key by checking against the project's translation
     * cache. If a match is found, it creates a NgxTranslateReference for navigation.
     * 
     * @param element The PSI element to potentially create a reference for
     * @param context The processing context for reference creation
     * @return Array of references (empty if the element is not a valid translation key)
     */
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference?> {
        val key = (element as? JSLiteralExpression)?.stringValue ?: return PsiReference.EMPTY_ARRAY
        val project = element.project
        val cache = project.getService(NgxTranslateTranslationCache::class.java)
        if (!cache.hasKey(key)) return PsiReference.EMPTY_ARRAY
        return arrayOf(NgxTranslateReference(element, key))
    }

}
