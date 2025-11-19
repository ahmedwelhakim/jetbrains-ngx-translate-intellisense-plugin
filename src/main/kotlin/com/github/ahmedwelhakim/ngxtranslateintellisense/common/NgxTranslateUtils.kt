package com.github.ahmedwelhakim.ngxtranslateintellisense.common

import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateConfigurationStateService
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Utility object providing common helper functions for the NgxTranslateIntellisense plugin.
 *
 * This object contains utility methods for file validation, path manipulation,
 * and text processing used across various components of the plugin.
 */
object NgxTranslateUtils {

    /**
     * Checks if a given virtual file is a valid translation file.
     *
     * A file is considered a translation file if it has a .json extension
     * and its name matches the locale pattern (e.g., "en.json", "fr-FR.json").
     *
     * @param file The virtual file to check
     * @return true if the file is a valid translation file, false otherwise
     */
    fun isTranslationFile(file: VirtualFile): Boolean {
        return file.extension == "json" && NgxTranslateConstants.LOCALE_PATTERN.matches(file.name)
    }

    /**
     * Checks if a translation directory contains valid translation files.
     *
     * The directory is considered non-empty if it contains at least one file
     * and all files in the directory are valid translation files.
     *
     * @param directoryPath The absolute path to the directory to check
     * @return true if the directory contains valid translation files, false otherwise
     */
    fun isTranslationDirectoryNotEmpty(directoryPath: String): Boolean {
        val dir = VirtualFileManager.getInstance().findFileByUrl("file://$directoryPath") ?: return false
        return dir.children?.isNotEmpty() == true && dir.children.any { it.extension == "json" }
    }

    /**
     * Truncates text to the configured maximum length for inlay hints.
     *
     * This method retrieves the configured maximum length from the project settings
     * and truncates the text accordingly. If the text exceeds the maximum length,
     * it appends "..." to indicate truncation.
     *
     * @param text The text to truncate (can be null)
     * @param project The IntelliJ project instance
     * @return The truncated text with ellipsis if needed, or null if input was null
     */
    fun getTruncatedValue(text: String?, project: Project): String? {
        val config = project.getService(NgxTranslateConfigurationStateService::class.java)
        val length = config.state.inlayHintLength
        return text?.take(length)
            ?.let { if (text.length > length) "$it..." else it }
    }

    /**
     * Converts a file path to system-independent format (using forward slashes).
     *
     * @param path The file path to convert
     * @return The system-independent path
     */
    fun toSystemIndependent(path: String): String = FileUtil.toSystemIndependentName(path)

    /**
     * Converts a list of file paths to system-independent format.
     *
     * @param list The mutable list of file paths to convert
     * @return A new mutable list with system-independent paths
     */
    fun toSystemIndependent(list: MutableList<String>): MutableList<String> =
        list.map { toSystemIndependent(it) }.toMutableList()

    /**
     * Checks if the project is an Angular project or Nx project that contains ngx-translate library.
     *
     * This method verifies:
     * 1. If the project has package.json with @ngx-translate/core dependency
     * 2. If it's an Angular project (has angular.json or @angular/core in package.json)
     * 3. If it's a Nx project (has nx.json or @nx/workspace in package.json)
     *
     * @param project The IntelliJ project instance
     * @return true if the project is Angular/Nx with ngx-translate, false otherwise
     */
    fun isAngularOrNxProjectWithNgxTranslate(project: Project): Boolean {
        val projectBasePath = project.basePath ?: return false

        // Quick check: only look at root package.json
        val packageJsonFile = VirtualFileManager.getInstance()
            .findFileByUrl("file://$projectBasePath/package.json") ?: return false

        return try {
            ReadAction.compute<String, Exception> {
                val psiFile = PsiManager.getInstance(project).findFile(packageJsonFile)
                psiFile?.text
            }?.let { content ->
                // Must have ngx-translate
                content.contains("@ngx-translate/core") &&
                        // Angular or Nx project
                        (content.contains("@angular/core") ||
                                Files.exists(Paths.get(projectBasePath, "angular.json")) ||
                                Files.exists(Paths.get(projectBasePath, "nx.json")) ||
                                content.contains("@nx/workspace"))
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
