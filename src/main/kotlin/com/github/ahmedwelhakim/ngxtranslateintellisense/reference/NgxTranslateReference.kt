package com.github.ahmedwelhakim.ngxtranslateintellisense.reference

import com.github.ahmedwelhakim.ngxtranslateintellisense.psi.NgxTranslatePsiUtils
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult


class NgxTranslateReference(
    element: JSLiteralExpression,
    private val key: String
) : PsiReferenceBase.Poly<JSLiteralExpression>(element, true) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return NgxTranslatePsiUtils.getTranslationValueStringLiterals(
            element.project,
            element.stringValue
                ?.split('.')
        )
            .map { PsiElementResolveResult(it) }
            .toTypedArray()
    }

}
