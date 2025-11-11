package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.services

import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.common.NgxTranslateConstants
import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.common.NgxTranslateUtils
import com.intellij.openapi.components.*
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

@State(name = "ConfigurationStateService", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
@Service(Service.Level.PROJECT)
class NgxTranslateConfigurationStateService(private val project: Project) :
    SimplePersistentStateComponent<NgxTranslateConfigurationStateService.ConfigurationState>(ConfigurationState()) {

    class ConfigurationState : BaseState() {
        var lang by string("en")
        var i18nPaths by list<String>()          // unified list
        var inlayHintLength by property(40)
        var inlayHintEnabled by property(true)
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

    fun saveSettings(
        lang: String,
        paths: MutableList<String>,
        inlayHintLength: Int,
        inlayHintEnabled: Boolean,
        autoDiscoveryEnabled: Boolean
    ) {
        state.lang = NgxTranslateUtils.toSystemIndependent(lang)
        state.i18nPaths = NgxTranslateUtils.toSystemIndependent(paths)
        state.inlayHintLength = inlayHintLength
        state.inlayHintEnabled = inlayHintEnabled
        state.autoDiscoveryEnabled = autoDiscoveryEnabled
    }

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

    private fun setI18nPaths(paths: List<String>) {
        state.i18nPaths = (state.i18nPaths + paths)
            .distinct()
            .filter(NgxTranslateUtils::isTranslationDirectoryNotEmpty)
            .toMutableList()
    }

    companion object {
        fun getInstance(project: Project): NgxTranslateConfigurationStateService =
            project.getService(NgxTranslateConfigurationStateService::class.java)


    }
}
