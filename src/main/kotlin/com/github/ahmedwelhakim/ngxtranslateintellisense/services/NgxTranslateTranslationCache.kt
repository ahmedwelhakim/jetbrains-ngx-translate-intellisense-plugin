package com.github.ahmedwelhakim.ngxtranslateintellisense.services

import com.github.ahmedwelhakim.ngxtranslateintellisense.psi.NgxTranslatePsiUtils
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.toArray

/**
 * Service that provides cached access to translation data for improved performance.
 * 
 * This service caches translation keys and values from JSON files to avoid
 * repeated file parsing operations. It automatically invalidates the cache
 * when translation files or configuration changes occur.
 * 
 * The cache is project-scoped and uses IntelliJ's CachedValuesManager
 * for efficient dependency tracking and automatic cache invalidation.
 */
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

    /**
     * Data holder for the cached snapshot of translation data.
     * 
     * @param keys Set of all available translation keys
     * @param values Map of translation keys to their corresponding string values
     */
    private data class Snapshot(
        val keys: Set<String>,
        val values: Map<String, String>
    )

    /**
     * Returns all translation keys from the cache.
     * 
     * @return Set of all available translation keys in the project
     */
    fun getAllKeys(): Set<String> = cachedSnapshot.value.keys

    /**
     * Checks if a specific translation key exists in the cache.
     * 
     * @param key The translation key to check
     * @return true if the key exists, false otherwise
     */
    fun hasKey(key: String): Boolean = cachedSnapshot.value.keys.contains(key)

    /**
     * Returns the translation value for a given key.
     * 
     * @param key The translation key to look up
     * @return The translation value, or null if the key doesn't exist
     */
    fun getValueForKey(key: String): String? = cachedSnapshot.value.values[key]

}
