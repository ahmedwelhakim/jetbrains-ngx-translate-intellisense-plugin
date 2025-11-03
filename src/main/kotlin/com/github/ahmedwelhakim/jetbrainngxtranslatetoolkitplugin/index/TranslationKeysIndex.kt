package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.index

import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.project.Project
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.indexing.*
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor

class TranslationKeysIndex  : ScalarIndexExtension<String>() {

    companion object {
        val NAME: ID<String, Void> = ID.create("translation.key.index")

        fun findAllKeys(project: Project): Set<String> {
            return FileBasedIndex.getInstance().getAllKeys(NAME, project).toSet()
        }

        fun containsKey(project: Project, key: String): Boolean {
            return FileBasedIndex.getInstance().getAllKeys(NAME, project).contains(key)
        }
    }

    override fun getName(): ID<String, Void> = NAME
    override fun getVersion(): Int = 1
    override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(JsonFileType.INSTANCE)

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