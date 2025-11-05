package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.services

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil

@State(name = "ConfigurationStateService", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
@Service(Service.Level.PROJECT)
class NgxTranslateConfigurationStateService :
    SimplePersistentStateComponent<NgxTranslateConfigurationStateService.ConfigurationState>(ConfigurationState()) {

    class ConfigurationState : BaseState() {
        var lang by string("en")
        var i18nPaths by list<String>()
        var inlayHintLength by property(40)
    }

    fun saveSettings(lang: String, path: MutableList<String>, inlayHintLength: Int) {
        state.lang = toSystemIndependent(lang)
        state.i18nPaths = toSystemIndependent(path)
        state.inlayHintLength = inlayHintLength
    }

    companion object {
        fun getInstance(project: Project): NgxTranslateConfigurationStateService =
            project.getService(NgxTranslateConfigurationStateService::class.java)

        fun getSystemDependentPath(project: Project): List<String> {
            return toSystemDependent(getInstance(project).state.i18nPaths)
        }

        private fun toSystemIndependent(path: String): String {
            return path.let { FileUtil.toSystemIndependentName(it) }
        }
        private fun toSystemIndependent(path: MutableList<String>): MutableList<String> {
            return path.map { toSystemIndependent(it) }.toMutableList()
        }
        private fun toSystemDependent(path: String): String {
            return path.let { FileUtil.toSystemDependentName(it) }
        }
        private fun toSystemDependent(path: MutableList<String>): MutableList<String> {
            return path.map { toSystemDependent(it) }.toMutableList()
        }
    }

}