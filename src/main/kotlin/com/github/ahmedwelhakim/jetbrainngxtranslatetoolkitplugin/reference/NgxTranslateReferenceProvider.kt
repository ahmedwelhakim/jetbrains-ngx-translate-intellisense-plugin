package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.reference

import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.services.NgxTranslateKeyCache
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext

class NgxTranslateReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference?> {
        val key = (element as? JSLiteralExpression)?.stringValue ?: return PsiReference.EMPTY_ARRAY
        val project = element.project
        val cache = project.getService(NgxTranslateKeyCache::class.java)
        if (!cache.hasKey(key)) return PsiReference.EMPTY_ARRAY
        return arrayOf(NgxTranslateReference(element, key))
    }

}