package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.services

import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.indexes.NgxTranslateKeysFileIndex
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.indexing.FileBasedIndex

@Service(Service.Level.PROJECT)
class NgxTranslateKeyCache(private val project: Project) {
    private val cachedValuesManager = CachedValuesManager.getManager(project)


    fun getAllKeys(): Set<String> {
        return cachedValuesManager.getCachedValue(project) {
            val keys = NgxTranslateKeysFileIndex.findAllKeys(project)

            CachedValueProvider.Result.create(
                keys,
                FileBasedIndex.getInstance().getIndexModificationStamp(
                    NgxTranslateKeysFileIndex.TRANSLATION_INDEX_ID,
                    project
                )
            )
        }
    }

    fun hasKey(key: String): Boolean = getAllKeys().contains(key)
}