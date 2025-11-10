package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.services

import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.psi.NgxTranslatePsiUtils
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.toArray

@Service(Service.Level.PROJECT)
class NgxTranslateTranslationCache(private val project: Project) {

    private val cachedValuesManager = CachedValuesManager.getManager(project)

    private val cachedSnapshot = cachedValuesManager.createCachedValue {
        val keys = NgxTranslatePsiUtils.getAllTranslationKeys(project).toSet()
        val values = NgxTranslatePsiUtils.getAllTranslationKeyValue(project)

        CachedValueProvider.Result.create(
            Snapshot(keys, values),
            listOf(
                *NgxTranslatePsiUtils.getTranslationJsonFilesFromProject(project).toArray(arrayOf()),
                NgxTranslateConfigurationStateService.getInstance(project).state
            )
        )
    }

    /** Data holder for the cached snapshot */
    private data class Snapshot(
        val keys: Set<String>,
        val values: Map<String, String>
    )

    /** Returns all translation keys (cached). */
    fun getAllKeys(): Set<String> = cachedSnapshot.value.keys

    /** Fast key lookup using cached set. */
    fun hasKey(key: String): Boolean = cachedSnapshot.value.keys.contains(key)

    /** Returns translation value. */
    fun getValueForKey(key: String): String? = cachedSnapshot.value.values[key]

}
