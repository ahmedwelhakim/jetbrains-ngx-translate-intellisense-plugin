package com.github.ahmedwelhakim.ngxtranslateintellisense.reference

import com.github.ahmedwelhakim.ngxtranslateintellisense.psi.NgxTranslatePsiUtils
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult


/**
 * Reference implementation for ngx-translate translation keys.
 * 
 * This class provides navigation from string literals in code to their corresponding
 * translation values in JSON files. It enables features like "Go to Declaration"
 * and "Find Usages" for translation keys.
 * 
 * The reference resolves dot-separated translation keys (e.g., "user.profile.name")
 * to the actual JSON string literal elements in the translation files.
 */
class NgxTranslateReference(
    element: JSLiteralExpression,
    private val key: String
) : PsiReferenceBase.Poly<JSLiteralExpression>(element, true) {
    /**
     * Resolves the reference to find all matching translation string literals.
     * 
     * This method navigates through JSON translation files to find the actual
     * string literal elements that correspond to the translation key in this reference.
     * 
     * @param incompleteCode Whether the code is incomplete (affects resolution behavior)
     * @return Array of resolve results containing the found JSON string literal elements
     */
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
