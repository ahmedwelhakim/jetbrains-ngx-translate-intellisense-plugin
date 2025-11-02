package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.reference

import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.psi.I18nPsiUtils
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult


class I18nReference(
    element: JSLiteralExpression,
    private val key: String
) : PsiReferenceBase.Poly<JSLiteralExpression>(element, true) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return I18nPsiUtils.getTranslationValueStringLiterals(
            element.project,
            element.stringValue
                ?.split('.')
        )
            .map { PsiElementResolveResult(it) }
            .toTypedArray()
    }

}