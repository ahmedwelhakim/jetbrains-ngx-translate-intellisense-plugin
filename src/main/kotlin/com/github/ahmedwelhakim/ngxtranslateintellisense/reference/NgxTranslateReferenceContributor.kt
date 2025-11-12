package com.github.ahmedwelhakim.ngxtranslateintellisense.reference

import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import org.angular2.lang.expr.Angular2Language

/**
 * Reference contributor that registers providers for ngx-translate translation key references.
 * 
 * This class is responsible for registering reference providers that enable navigation
 * and code intelligence features for translation keys. It registers providers for both
 * regular JavaScript string literals and Angular expression contexts.
 * 
 * The contributor enables features like:
 * - Go to Declaration for translation keys
 * - Find Usages of translation keys
 * - Code navigation from code to translation files
 */
class NgxTranslateReferenceContributor : PsiReferenceContributor() {
    /**
     * Registers reference providers for translation key string literals.
     * 
     * This method registers the NgxTranslateReferenceProvider for both JavaScript
     * and Angular expression contexts, enabling reference resolution for translation
     * keys in various code contexts.
     * 
     * @param registrar The reference registrar to register providers with
     */
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(JSLiteralExpression::class.java),
            NgxTranslateReferenceProvider()
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(JSLiteralExpression::class.java).withLanguage(Angular2Language),
            NgxTranslateReferenceProvider()
        )
    }
}
