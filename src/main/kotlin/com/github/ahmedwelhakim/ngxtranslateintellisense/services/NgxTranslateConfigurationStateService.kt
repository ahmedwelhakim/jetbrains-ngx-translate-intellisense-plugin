package com.github.ahmedwelhakim.ngxtranslateintellisense.services

import com.github.ahmedwelhakim.ngxtranslateintellisense.common.NgxTranslateConstants
import com.github.ahmedwelhakim.ngxtranslateintellisense.common.NgxTranslateUtils
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

/**
 * Service class that manages the persistent configuration state for the NgxTranslateIntellisense plugin.
 * 
 * This service handles:
 * - Storing and retrieving plugin configuration settings
 * - Auto-discovery of translation directories in the project
 * - File system monitoring for automatic path updates
 * - Persistence of settings across IDE sessions
 * 
 * The service is project-scoped and stores its state in the workspace file,
 * allowing each project to have its own independent configuration.
 */
@State(name = "ConfigurationStateService", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
@Service(Service.Level.PROJECT)
class NgxTranslateConfigurationStateService(private val project: Project) :
    SimplePersistentStateComponent<NgxTranslateConfigurationStateService.ConfigurationState>(ConfigurationState()) {

    /**
     * Configuration state class that holds all plugin settings.
     * 
     * This class extends BaseState to provide automatic persistence
     * and change notification capabilities for all configuration properties.
     */
    class ConfigurationState : BaseState() {
        var lang by string("en")
        var i18nPaths by list<String>()          // unified list
        var inlayHintLength by property(40)
        var inlayHintEnabled by property(true)
        var foldKeyEnabled by property(false)
        var autoDiscoveryEnabled by property(true)
    }

    init {
        autoDiscoverPaths()
        project.messageBus.connect().subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: MutableList<out VFileEvent>) {
                    if (!state.autoDiscoveryEnabled) return

                    if (events.any { it.file?.extension == "json" }) {
                        autoDiscoverPaths()

                    }
                }
            }
        )
    }

    /**
     * Saves the provided settings to the persistent configuration state.
     * 
     * @param lang The default language code (e.g., "en", "fr")
     * @param paths List of translation directory paths
     * @param inlayHintLength Maximum length for inlay hint text
     * @param inlayHintEnabled Whether inlay hints are enabled
     * @param autoDiscoveryEnabled Whether auto-discovery of paths is enabled
     */
    fun saveSettings(
        lang: String,
        paths: MutableList<String>,
        inlayHintLength: Int,
        inlayHintEnabled: Boolean,
        foldKeyEnabled: Boolean,
        autoDiscoveryEnabled: Boolean
    ) {
        state.lang = NgxTranslateUtils.toSystemIndependent(lang)
        state.i18nPaths = NgxTranslateUtils.toSystemIndependent(paths)
        state.inlayHintLength = inlayHintLength
        state.inlayHintEnabled = inlayHintEnabled
        state.foldKeyEnabled = foldKeyEnabled
        state.autoDiscoveryEnabled = autoDiscoveryEnabled

        DaemonCodeAnalyzer.getInstance(project).restart()
        ApplicationManager.getApplication().invokeLater {
            val foldingManager = CodeFoldingManager.getInstance(project)
            EditorFactory.getInstance().allEditors
                .filter { it.project == project }
                .forEach { foldingManager.updateFoldRegions(it) }
        }
    }

    /**
     * Automatically discovers translation directories in the project.
     * 
     * This method scans all project content roots, looking for directories
     * that contain locale-specific JSON files. It excludes common build and
     * dependency directories to improve performance and avoid false positives.
     */
    fun autoDiscoverPaths() {
        if (!state.autoDiscoveryEnabled) return
        val result = mutableSetOf<String>()


        fun scan(dir: VirtualFile) {
            if (!dir.isDirectory) return
            if (dir.name in NgxTranslateConstants.EXCLUDED_DIRS_IN_PATH) return
            val segments = dir.path.split('/', '\\')
            if (segments.any { it in NgxTranslateConstants.EXCLUDED_DIRS_IN_PATH }) return

            val children = dir.children ?: return

            val localeJsonCount =
                children.count(NgxTranslateUtils::isTranslationFile)

            if (localeJsonCount >= 1) {
                result += dir.path
            }

            for (child in children) if (child.isDirectory) scan(child)
        }

        val modules = ModuleManager.getInstance(project).modules
        for (module in modules) {
            val roots = ModuleRootManager.getInstance(module).contentRoots
            for (root in roots) scan(root)
        }
        if (result.isNotEmpty())
            setI18nPaths(result.toList())
    }

    /**
     * Updates the i18n paths list with new discovered paths.
     * 
     * This method merges new paths with existing ones, removes duplicates,
     * and filters out directories that don't contain valid translation files.
     * 
     * @param paths List of new translation directory paths to add
     */
    private fun setI18nPaths(paths: List<String>) {
        state.i18nPaths = (state.i18nPaths + paths)
            .distinct()
            .filter(NgxTranslateUtils::isTranslationDirectoryNotEmpty)
            .toMutableList()
    }

    /**
     * Companion object providing access to the service instance.
     */
    companion object {
        fun getInstance(project: Project): NgxTranslateConfigurationStateService =
            project.getService(NgxTranslateConfigurationStateService::class.java)


    }
}
