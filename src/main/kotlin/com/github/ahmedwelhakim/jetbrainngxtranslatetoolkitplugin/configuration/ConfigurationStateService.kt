package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.configuration

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil

@State(name = "ConfigurationStateService", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
@Service(Service.Level.PROJECT)
class ConfigurationStateService :
    SimplePersistentStateComponent<ConfigurationStateService.ConfigurationState>(ConfigurationState()) {

    class ConfigurationState : BaseState() {
        var lang by string("en")
        var i18nPath by string("")
    }

    fun saveState(lang: String, path: String) {
        state.lang = toSystemIndependent(lang)
        state.i18nPath = toSystemIndependent(path)
    }

    companion object {
        fun getInstance(project: Project): ConfigurationStateService =
            project.getService(ConfigurationStateService::class.java)

        fun getSystemDependentPath(project: Project): String {
            return toSystemDependent(getInstance(project).state.i18nPath!!)
        }

        private fun toSystemIndependent(path: String): String {
            return path.let { FileUtil.toSystemIndependentName(it) }
        }

        private fun toSystemDependent(path: String): String {
            return path.let { FileUtil.toSystemDependentName(it) }
        }
    }

}