package com.github.ahmedwelhakim.ngxtranslateintellisense.navigation

import com.github.ahmedwelhakim.ngxtranslateintellisense.common.NgxTranslateUtils
import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateTranslationCache
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.TextOccurenceProcessor
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.pom.Navigatable
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.DefaultListModel
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class NgxTranslateJsonKeyGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        val element = sourceElement ?: return null
        val project = element.project

        if (!NgxTranslateUtils.isSupportedProject(project)) return null

        val file = element.containingFile ?: return null
        val virtualFile = file.virtualFile ?: return null
        if (!NgxTranslateUtils.isTranslationFile(virtualFile)) return null

        val property = PsiTreeUtil.getParentOfType(element, JsonProperty::class.java, false) ?: return null
        val nameElement = property.nameElement
        if (!PsiTreeUtil.isAncestor(nameElement, element, false)) return null

        val fullKey = computeFullKey(property)
        val cache = project.getService(NgxTranslateTranslationCache::class.java)
        if (!cache.hasKey(fullKey)) return null

        val results = LinkedHashSet<PsiElement>()
        val searchHelper = PsiSearchHelper.getInstance(project)
        val scope = GlobalSearchScope.projectScope(project)
        val seed = fullKey.substringBefore('.')

        searchHelper.processElementsWithWord(
            TextOccurenceProcessor { psiElement, _ ->
                val literal = psiElement as? JSLiteralExpression ?: return@TextOccurenceProcessor true
                val literalKey = literal.stringValue ?: return@TextOccurenceProcessor true
                if (literalKey == fullKey) results.add(literal)
                true
            },
            scope,
            seed,
            UsageSearchContext.IN_STRINGS,
            true
        )

        if (results.isEmpty()) return null

        if (results.size == 1) return arrayOf(results.first())

        val pointerManager = SmartPointerManager.getInstance(project)
        val pointers = results.map { pointerManager.createSmartPsiElementPointer(it) }
        return arrayOf(NgxTranslatePopupTarget(element, editor, fullKey, pointers))
    }

    override fun getActionText(context: com.intellij.openapi.actionSystem.DataContext): String? = null

    private fun computeFullKey(property: JsonProperty): String {
        val segments = mutableListOf(property.name)
        var parentObject = property.parent as? JsonObject
        while (true) {
            val parentProperty = parentObject?.parent as? JsonProperty ?: break
            segments.add(parentProperty.name)
            parentObject = parentProperty.parent as? JsonObject
        }
        return segments.asReversed().joinToString(".")
    }

    private data class UsageEntry(
        val target: PsiElement,
        val fileName: String,
        val line: Int?,
        val lineText: String,
        val relativePath: String
    )

    private fun showUsagesPopup(editor: Editor, fullKey: String, targets: List<SmartPsiElementPointer<PsiElement>>) {
        val project = editor.project ?: return
        val entries = ReadAction.compute<List<UsageEntry>, RuntimeException> {
            targets.mapNotNull { it.element }.mapNotNull { target ->
                val file = target.containingFile ?: return@mapNotNull null
                val vFile = file.virtualFile ?: return@mapNotNull null
                val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return@mapNotNull null
                val startOffset = target.textRange?.startOffset ?: 0
                val lineNumber = document.getLineNumber(startOffset)
                val lineText = normalizeLineText(getLineText(document, lineNumber))
                val basePath = project.basePath?.trimEnd('/')
                val path = vFile.path
                val relativePath = if (!basePath.isNullOrBlank() && path.startsWith("$basePath/")) {
                    path.removePrefix("$basePath/")
                } else {
                    path
                }
                UsageEntry(
                    target = target,
                    fileName = vFile.name,
                    line = lineNumber + 1,
                    lineText = lineText,
                    relativePath = relativePath
                )
            }
        }

        val model = DefaultListModel<UsageEntry>().apply { entries.forEach { addElement(it) } }
        val leftColumnWidth = run {
            val fm = editor.contentComponent.getFontMetrics(editor.contentComponent.font)
            val maxTextWidth = entries.maxOfOrNull {
                fm.stringWidth("${it.fileName}:${it.line?.toString() ?: "?"}")
            } ?: 0
            maxTextWidth + JBUI.scale(24)
        }
        val list = JBList(model).apply {
            cellRenderer = UsageEntryRenderer(fullKey, leftColumnWidth)
            visibleRowCount = 8
        }

        val pathLabel = JBLabel().apply {
            border = JBUI.Borders.empty(4, 8)
        }
        fun updatePathLabel(entry: UsageEntry?) {
            pathLabel.text = entry?.relativePath ?: ""
        }
        if (entries.isNotEmpty()) {
            list.selectedIndex = 0
            updatePathLabel(entries.first())
        }

        list.addListSelectionListener {
            updatePathLabel(list.selectedValue)
        }

        val south = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.customLineTop(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())
            add(pathLabel, BorderLayout.CENTER)
        }

        val popup: JBPopup = PopupChooserBuilder(list)
            .setTitle("Select Usage")
            .setResizable(true)
            .setMovable(true)
            .setMinSize(Dimension(JBUI.scale(720), JBUI.scale(160)))
            .setDimensionServiceKey("ngxTranslate.gotoDeclaration.popup")
            .setSouthComponent(south)
            .setItemChosenCallback(Runnable {
                val selected = list.selectedValue ?: return@Runnable
                (selected.target as? Navigatable)?.navigate(true)
                    ?: (selected.target.navigationElement as? Navigatable)?.navigate(true)
            })
            .createPopup()

        popup.showInBestPositionFor(editor)
    }

    private fun getLineText(document: Document, lineNumber: Int): String {
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        return document.charsSequence.subSequence(lineStart, lineEnd).toString()
    }

    private fun normalizeLineText(text: String): String {
        return text
            .replace('\t', ' ')
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun buildSnippet(text: String, match: String, context: Int): Triple<String, String, String> {
        val idx = text.indexOf(match)
        if (idx < 0) {
            if (text.length <= context * 2) return Triple(text, "", "")
            val head = text.take(context)
            val tail = text.takeLast(context)
            return Triple("$head...", "", "...$tail")
        }

        val start = (idx - context).coerceAtLeast(0)
        val end = (idx + match.length + context).coerceAtMost(text.length)

        val prefix = buildString {
            if (start > 0) append("...")
            append(text.substring(start, idx))
        }
        val suffix = buildString {
            append(text.substring(idx + match.length, end))
            if (end < text.length) append("...")
        }

        return Triple(prefix, match, suffix)
    }

    private inner class UsageEntryRenderer(
        private val fullKey: String,
        private val leftColumnWidth: Int
    ) : ListCellRenderer<UsageEntry> {
        private val panel = JPanel(BorderLayout())
        private val left = SimpleColoredComponent()
        private val right = SimpleColoredComponent()
        private val leftWrapper = JPanel(BorderLayout())

        init {
            panel.border = JBUI.Borders.empty(2, 8)
            left.isOpaque = false
            right.isOpaque = false
            val noPad = JBUI.insets(0)
            left.setIpad(noPad)
            right.setIpad(noPad)
            left.border = null
            right.border = null
            left.setIconTextGap(0)
            right.setIconTextGap(0)

            leftWrapper.isOpaque = false
            leftWrapper.border = JBUI.Borders.emptyRight(12)
            leftWrapper.add(left, BorderLayout.CENTER)

            panel.add(leftWrapper, BorderLayout.WEST)
            panel.add(right, BorderLayout.CENTER)
        }

        override fun getListCellRendererComponent(
            list: javax.swing.JList<out UsageEntry>,
            value: UsageEntry,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            left.clear()
            right.clear()

            val fileAndLine = buildString {
                append(value.fileName)
                append(':')
                append(value.line?.toString() ?: "?")
            }
            left.append(fileAndLine, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            leftWrapper.preferredSize = Dimension(leftColumnWidth, leftWrapper.preferredSize.height)

            val (before, match, after) = buildSnippet(value.lineText, fullKey, 30)
            if (before.isNotEmpty()) right.append(before, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            if (match.isNotEmpty()) right.append(match, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            if (after.isNotEmpty()) right.append(after, SimpleTextAttributes.REGULAR_ATTRIBUTES)

            panel.background = if (isSelected) list.selectionBackground else list.background
            panel.isOpaque = true
            left.foreground = if (isSelected) list.selectionForeground else list.foreground
            right.foreground = if (isSelected) list.selectionForeground else list.foreground

            return panel
        }
    }

    private inner class NgxTranslatePopupTarget(
        private val anchor: PsiElement,
        private val editor: Editor,
        private val fullKey: String,
        private val pointers: List<SmartPsiElementPointer<PsiElement>>
    ) : FakePsiElement(), Navigatable {
        override fun getParent(): PsiElement = anchor

        override fun getProject() = anchor.project

        override fun getNavigationElement(): PsiElement = this

        override fun navigate(requestFocus: Boolean) {
            val project = anchor.project
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater
                if (editor.isDisposed) return@invokeLater
                showUsagesPopup(editor, fullKey, pointers)
            }
        }

        override fun canNavigate(): Boolean = true

        override fun canNavigateToSource(): Boolean = false
    }
}
