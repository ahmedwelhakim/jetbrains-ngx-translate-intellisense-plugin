package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.indexes

import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.services.NgxTranslateConfigurationStateService
import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.*
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor

class NgxTranslateKeysFileIndex : ScalarIndexExtension<String>() {

    companion object {
        val TRANSLATION_INDEX_ID: ID<String, Void> = ID.create("ngxTranslateToolset.key.index")

        fun findAllKeys(project: Project): Set<String> {
            return FileBasedIndex.getInstance().getAllKeys(TRANSLATION_INDEX_ID, project).toSet()
        }
        
    }

    override fun getName(): ID<String, Void> = TRANSLATION_INDEX_ID
    override fun getVersion(): Int = 1
    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return object : FileBasedIndex.InputFilter {
            override fun acceptInput(file: VirtualFile): Boolean {
                if (file.fileType != JsonFileType.INSTANCE) return false


                val path = file.path
                if (path.contains("/node_modules/") ||
                    path.contains("/dist/") ||
                    path.contains("/build/")
                ) {
                    return false
                }

                val project = ProjectLocator.getInstance().guessProjectForFile(file) ?: return false
                val config = NgxTranslateConfigurationStateService.getInstance(project)
                val i18nPath = config.state.i18nPath ?: "i18n"
                val res = path.contains(i18nPath)
                return path.contains(i18nPath)
            }
        }
    }

    override fun dependsOnFileContent() = true

    override fun getIndexer(): DataIndexer<String, Void, FileContent> = DataIndexer { input ->
        val map = mutableMapOf<String, Void?>()
        val psiFile = input.psiFile as? JsonFile ?: return@DataIndexer map

        val root = psiFile.topLevelValue as? JsonObject ?: return@DataIndexer map
        collectKeys("", root, map)
        map
    }

    private fun collectKeys(prefix: String, json: JsonObject, map: MutableMap<String, Void?>) {
        for (property in json.propertyList) {
            val key = if (prefix.isEmpty()) property.name else "$prefix.${property.name}"
            val value = property.value
            if (value is JsonObject) {
                collectKeys(key, value, map)
            } else {
                map[key] = null
            }
        }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE
}