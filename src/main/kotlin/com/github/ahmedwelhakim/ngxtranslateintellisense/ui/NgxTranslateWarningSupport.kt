package com.github.ahmedwelhakim.ngxtranslateintellisense.ui

import com.github.ahmedwelhakim.ngxtranslateintellisense.NgxTranslateIntellisenseBundle
import com.github.ahmedwelhakim.ngxtranslateintellisense.psi.NgxTranslatePsiUtils
import com.github.ahmedwelhakim.ngxtranslateintellisense.psi.NgxTranslatePsiUtils.TranslationKeyMismatch
import com.github.ahmedwelhakim.ngxtranslateintellisense.psi.NgxTranslatePsiUtils.TranslationKeyMismatchType
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal class NgxTranslateWarningSupport(private val project: Project) {
    companion object {
        private const val WARNING_LINK_SCHEME = "ngx-translate-warning://open"
        private const val KEY_PREVIEW_LIMIT = 10
    }

    fun emptyWarningsHtml(): String {
        val noneMessage = NgxTranslateIntellisenseBundle.message("translationKeyWarningsNone")
        return "<html><body>${StringUtil.escapeXmlEntities(noneMessage)}</body></html>"
    }

    fun buildMismatchWarningHtml(paths: List<String>): String {
        val folderBlocks = paths.mapNotNull { path ->
            val consistency = NgxTranslatePsiUtils.validateTranslationKeysConsistency(project, path)
            if (consistency.isValid) return@mapNotNull null

            val fileLines = consistency.mismatchDetails.entries
                .sortedBy { it.key }
                .joinToString("") { (fileName, mismatches) ->
                    val missingHtml = buildMismatchGroupHtml(
                        directoryPath = path,
                        label = NgxTranslateIntellisenseBundle.message("translationKeyWarningsMissing"),
                        mismatches = mismatches.filter { it.type == TranslationKeyMismatchType.MISSING }
                    )
                    val extraHtml = buildMismatchGroupHtml(
                        directoryPath = path,
                        label = NgxTranslateIntellisenseBundle.message("translationKeyWarningsExtra"),
                        mismatches = mismatches.filter { it.type == TranslationKeyMismatchType.EXTRA }
                    )

                    """
                    <li style='margin-bottom:6px;'>
                      <b>${StringUtil.escapeXmlEntities(fileName)}</b>
                      $missingHtml
                      $extraHtml
                    </li>
                    """.trimIndent()
                }

            """
            <div style='margin-bottom:8px;'>
              <div><b>${
                StringUtil.escapeXmlEntities(
                    NgxTranslateIntellisenseBundle.message(
                        "translationKeyWarningsFolder",
                        path
                    )
                )
            }</b></div>
              <ul style='margin-top:4px;'>$fileLines</ul>
            </div>
            """.trimIndent()
        }

        if (folderBlocks.isEmpty()) return ""
        return "<html><body>${folderBlocks.joinToString("")}</body></html>"
    }

    fun navigateFromWarningLink(description: String?) {
        if (description.isNullOrBlank() || !description.startsWith(WARNING_LINK_SCHEME)) return

        val query = description.substringAfter('?', missingDelimiterValue = "")
        val params = query
            .split('&')
            .mapNotNull { entry ->
                val index = entry.indexOf('=')
                if (index <= 0) return@mapNotNull null
                val key = entry.substring(0, index)
                val value = entry.substring(index + 1)
                key to URLDecoder.decode(value, StandardCharsets.UTF_8)
            }
            .toMap()

        val directoryPath = params["dir"] ?: return
        val fileName = params["file"] ?: return
        val key = params["key"] ?: return
        val target = NgxTranslatePsiUtils.findBestNavigationElement(project, directoryPath, fileName, key) ?: return
        val virtualFile = target.containingFile?.virtualFile ?: return
        val offset = target.textRange?.startOffset ?: 0
        OpenFileDescriptor(project, virtualFile, offset).navigate(true)
    }

    private fun buildMismatchGroupHtml(
        directoryPath: String,
        label: String,
        mismatches: List<TranslationKeyMismatch>
    ): String {
        if (mismatches.isEmpty()) return ""

        val previewLinks = mismatches
            .take(KEY_PREVIEW_LIMIT)
            .joinToString("") {
                buildWarningKeyLink(
                    directoryPath,
                    it.navigationFileName,
                    it.key,
                    it.type == TranslationKeyMismatchType.EXTRA
                )
            }
        val more = if (mismatches.size > KEY_PREVIEW_LIMIT) {
            StringUtil.escapeXmlEntities(
                NgxTranslateIntellisenseBundle.message(
                    "translationKeyWarningsMore",
                    mismatches.size - KEY_PREVIEW_LIMIT
                )
            )
        } else {
            ""
        }

        return "<div style='margin:2px 0 0 14px;'><b>${StringUtil.escapeXmlEntities(label)}</b> $previewLinks$more</div>"
    }

    private fun buildWarningKeyLink(directoryPath: String, fileName: String, key: String, isExtra: Boolean): String {
        val href = buildString {
            append(WARNING_LINK_SCHEME)
            append("?dir=")
            append(URLEncoder.encode(directoryPath, StandardCharsets.UTF_8))
            append("&file=")
            append(URLEncoder.encode(fileName, StandardCharsets.UTF_8))
            append("&key=")
            append(URLEncoder.encode(key, StandardCharsets.UTF_8))
        }

        return if (isExtra)
            "<div> <a href='$href'>${StringUtil.escapeXmlEntities(key)}</a> </div>"
        else
            "<div> ${StringUtil.escapeXmlEntities(key)} </div>"
    }
}


