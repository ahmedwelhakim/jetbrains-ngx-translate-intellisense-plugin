package com.github.ahmedwelhakim.ngxtranslateintellisense.folding

import com.github.ahmedwelhakim.ngxtranslateintellisense.common.NgxTranslateUtils
import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateConfigurationStateService
import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateTranslationCache
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import java.util.Collections
import java.util.WeakHashMap

class NgxTranslateFoldingBuilder : FoldingBuilderEx() {
    companion object {
        private val ngxTranslateGroups: MutableSet<FoldingGroup> =
            Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap()))

        fun isNgxTranslateGroup(group: FoldingGroup?): Boolean {
            return group != null && ngxTranslateGroups.contains(group)
        }

        private fun registerGroup(group: FoldingGroup) {
            ngxTranslateGroups.add(group)
        }
    }

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val project = root.project
        if (!NgxTranslateUtils.isAngularOrNxProjectWithNgxTranslate(project)) return emptyArray()

        val config = project.getService(NgxTranslateConfigurationStateService::class.java)
        if (!config.state.foldKeyEnabled) return emptyArray()

        project.getService(NgxTranslateFoldingAutoCollapseService::class.java)

        val cache = project.getService(NgxTranslateTranslationCache::class.java)
        val descriptors = ArrayList<FoldingDescriptor>()

        root.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                val literal = element as? JSLiteralExpression
                if (literal != null) {
                    val key = literal.stringValue
                    if (!key.isNullOrBlank() && cache.hasKey(key)) {
                        val value = NgxTranslateUtils.getTruncatedValue(cache.getValueForKey(key), project)
                        if (!value.isNullOrBlank()) {
                            val placeholder = value
                                .replace('\n', ' ')
                                .replace('\r', ' ')
                                .replace('\t', ' ')
                                .ifBlank { key }

                            val group = FoldingGroup.newGroup("ngxTranslateTranslationKey:${literal.textRange.startOffset}")
                            registerGroup(group)

                            val text = literal.text
                            val quote = text.firstOrNull()
                            val isStandardQuoted =
                                text.length >= 2 && (quote == '\'' || quote == '"') && text.lastOrNull() == quote

                            if (isStandardQuoted && literal.textRange.length >= 2) {
                                val start = literal.textRange.startOffset
                                val end = literal.textRange.endOffset
                                val openQuoteRange = TextRange(start, start + 1)
                                val keyRange = TextRange(start + 1, end - 1)
                                val closeQuoteRange = TextRange(end - 1, end)

                                val hidden = "\u200B"
                                descriptors.add(FoldingDescriptor(literal.node, openQuoteRange, group, hidden))
                                if (keyRange.length > 0) {
                                    descriptors.add(FoldingDescriptor(literal.node, keyRange, group, placeholder))
                                }
                                descriptors.add(FoldingDescriptor(literal.node, closeQuoteRange, group, hidden))
                            } else {
                                descriptors.add(FoldingDescriptor(literal.node, literal.textRange, group, placeholder))
                            }
                        }
                    }
                }
                super.visitElement(element)
            }
        })

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String {
        val literal = node.psi as? JSLiteralExpression ?: return "…"
        val key = literal.stringValue ?: return "…"
        val project = literal.project
        val cache = project.getService(NgxTranslateTranslationCache::class.java)
        val value = NgxTranslateUtils.getTruncatedValue(cache.getValueForKey(key), project)

        return (value ?: key)
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace('\t', ' ')
            .ifBlank { key }
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        val project = node.psi.project
        val config = project.getService(NgxTranslateConfigurationStateService::class.java)
        return config.state.foldKeyEnabled
    }
}
