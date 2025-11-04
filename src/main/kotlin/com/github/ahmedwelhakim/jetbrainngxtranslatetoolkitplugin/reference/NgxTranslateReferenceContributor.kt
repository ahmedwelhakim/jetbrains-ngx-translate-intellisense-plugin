package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.reference

import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import org.angular2.lang.expr.Angular2Language

class NgxTranslateReferenceContributor : PsiReferenceContributor() {
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