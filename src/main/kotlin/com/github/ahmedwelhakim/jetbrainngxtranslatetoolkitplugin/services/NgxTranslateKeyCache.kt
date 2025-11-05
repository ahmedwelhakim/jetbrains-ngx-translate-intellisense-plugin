package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.services

import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.indexes.NgxTranslateKeysFileIndex
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.indexing.FileBasedIndex

@Service(Service.Level.PROJECT)
class NgxTranslateKeyCache(private val project: Project) {

    private val cachedValuesManager = CachedValuesManager.getManager(project)
    val index: FileBasedIndex = FileBasedIndex.getInstance()
    val modificationTracker = ModificationTracker {
        index.getIndexModificationStamp(NgxTranslateKeysFileIndex.TRANSLATION_INDEX_ID, project)
    }

    /**
     * Cached snapshot of all translation keys (and optionally values).
     * Recomputed only when the translation index changes.
     */
    private val cachedSnapshot = cachedValuesManager.createCachedValue {
        val index = FileBasedIndex.getInstance()
        val psiManager = PsiManager.getInstance(project)
        val scope = GlobalSearchScope.projectScope(project)
        val keys = mutableSetOf<String>()
        val values = mutableMapOf<String, String>()

        // Collect all keys once from the index
        val allIndexedKeys = index.getAllKeys(NgxTranslateKeysFileIndex.TRANSLATION_INDEX_ID, project)
        for (key in allIndexedKeys) {
            keys += key

            val files = mutableListOf<VirtualFile>()
            index.getFilesWithKey(
                NgxTranslateKeysFileIndex.TRANSLATION_INDEX_ID,
                setOf(key),
                { vf ->
                    files += vf
                    true // continue processing
                },
                scope,
            )
            for (vf in files) {
                val psi = psiManager.findFile(vf) as? JsonFile ?: continue
                val json = psi.topLevelValue as? JsonObject ?: continue
                val value = extractValueForKey(json, key)
                if (value != null) {
                    values[key] = value
                    break
                }
            }
        }

        val dependency = modificationTracker

        CachedValueProvider.Result.create(Snapshot(keys, values), dependency)
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

    /** Returns translation value (if extracted). */
    fun getValueForKey(key: String): String? = cachedSnapshot.value.values[key]

    /** Helper to extract nested JSON value for a given dotted key (e.g. "home.title") */
    private fun extractValueForKey(root: JsonObject, dottedKey: String): String? {
        val parts = dottedKey.split(".")
        var current: JsonObject? = root

        for ((i, part) in parts.withIndex()) {
            val prop = current?.findProperty(part) ?: return null
            val value = prop.value
            if (i == parts.lastIndex) {
                return value?.text?.trim('"')
            }
            current = value as? JsonObject
        }
        return null
    }
}
