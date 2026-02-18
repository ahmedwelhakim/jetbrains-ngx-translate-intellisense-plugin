package com.github.ahmedwelhakim.ngxtranslateintellisense.folding

import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateConfigurationStateService
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.project.Project
import java.awt.event.MouseEvent
import java.lang.ref.WeakReference

@Service(Service.Level.PROJECT)
class NgxTranslateFoldingAutoCollapseService(private val project: Project) : Disposable {
    private var lastEditorRef: WeakReference<Editor>? = null

    init {
        EditorFactory.getInstance().eventMulticaster.addEditorMouseListener(object : EditorMouseListener {
            override fun mouseClicked(event: EditorMouseEvent) {
                val editor = event.editor
                if (editor.project != project) return

                val config = project.getService(NgxTranslateConfigurationStateService::class.java)
                if (!config.state.foldKeyEnabled) return

                val mouseEvent = event.mouseEvent
                if (mouseEvent.button != MouseEvent.BUTTON1) return

                val offset = editor.logicalPositionToOffset(editor.xyToLogicalPosition(mouseEvent.point))
                val region = editor.foldingModel.getCollapsedRegionAtOffset(offset) ?: return
                if (!NgxTranslateFoldingBuilder.isNgxTranslateGroup(region.group)) return
                val group = region.group

                ApplicationManager.getApplication().invokeLater {
                    if (editor.isDisposed) return@invokeLater
                    editor.foldingModel.runBatchFoldingOperation {
                        for (r in editor.foldingModel.allFoldRegions) {
                            if (r.group != group) continue
                            r.isExpanded = true
                        }
                    }
                }
            }
        }, this)

        EditorFactory.getInstance().eventMulticaster.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                val editor = event.editor
                if (editor.project != project) return

                val config = project.getService(NgxTranslateConfigurationStateService::class.java)
                if (!config.state.foldKeyEnabled) return

                val previousEditor = lastEditorRef?.get()
                if (previousEditor != null && previousEditor != editor) {
                    collapseAllTranslationFoldRegions(previousEditor)
                }

                lastEditorRef = WeakReference(editor)

                val document = editor.document
                val caretLine = document.getLineNumber(editor.caretModel.offset)

                ApplicationManager.getApplication().invokeLater {
                    if (editor.isDisposed) return@invokeLater
                    editor.foldingModel.runBatchFoldingOperation {
                        for (region in editor.foldingModel.allFoldRegions) {
                            if (!NgxTranslateFoldingBuilder.isNgxTranslateGroup(region.group)) continue
                            if (!region.isExpanded) continue
                            if (document.getLineNumber(region.startOffset) == caretLine) continue
                            region.isExpanded = false
                        }
                    }
                }
            }
        }, this)
    }

    private fun collapseAllTranslationFoldRegions(editor: Editor) {
        ApplicationManager.getApplication().invokeLater {
            if (editor.isDisposed) return@invokeLater
            editor.foldingModel.runBatchFoldingOperation {
                for (region in editor.foldingModel.allFoldRegions) {
                    if (!NgxTranslateFoldingBuilder.isNgxTranslateGroup(region.group)) continue
                    if (!region.isExpanded) continue
                    region.isExpanded = false
                }
            }
        }
    }

    override fun dispose() {}
}
