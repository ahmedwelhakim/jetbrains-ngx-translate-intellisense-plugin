package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.reference

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
        val literal = (element as? JSLiteralExpression)?.stringValue ?: return PsiReference.EMPTY_ARRAY

        return arrayOf(NgxTranslateReference(element, literal))
    }

}