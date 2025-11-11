package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.common

import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.services.NgxTranslateConfigurationStateService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager

object NgxTranslateUtils {

    fun isTranslationFile(file: VirtualFile): Boolean {
        return file.extension == "json" && NgxTranslateConstants.LOCALE_PATTERN.matches(file.name)
    }

    fun isTranslationDirectoryNotEmpty(directoryPath: String): Boolean {
        val dir = VirtualFileManager.getInstance().findFileByUrl("file://$directoryPath") ?: return false
        return dir.children?.isNotEmpty() == true && dir.children.all(::isTranslationFile)
    }

    fun getTruncatedValue(text: String?, project: Project): String? {
        val config = project.getService(NgxTranslateConfigurationStateService::class.java)
        val length = config.state.inlayHintLength
        return text?.take(length)
            ?.let { if (text.length > length) "$it..." else it }
    }

    fun toSystemIndependent(path: String): String = FileUtil.toSystemIndependentName(path)
    fun toSystemIndependent(list: MutableList<String>): MutableList<String> =
        list.map { toSystemIndependent(it) }.toMutableList()
}