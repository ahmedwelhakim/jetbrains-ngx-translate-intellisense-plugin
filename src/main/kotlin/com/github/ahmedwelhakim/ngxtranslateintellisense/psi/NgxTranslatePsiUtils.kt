package com.github.ahmedwelhakim.ngxtranslateintellisense.psi

import com.github.ahmedwelhakim.ngxtranslateintellisense.services.NgxTranslateConfigurationStateService
import com.intellij.json.JsonFileType
import com.intellij.json.JsonUtil
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.json.psi.JsonValue
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import kotlin.io.path.Path

/**
 * Utility object for PSI (Program Structure Interface) operations related to translation files.
 * 
 * This object provides methods for parsing JSON translation files, extracting translation keys
 * and values, and navigating the PSI tree structure to find translation string literals.
 * It serves as the core engine for file content analysis throughout the plugin.
 */
object NgxTranslatePsiUtils {
    /**
     * Retrieves JSON string literal elements for the specified translation keys.
     * 
     * This method navigates through JSON translation files to find the actual string
     * literal PSI elements that correspond to the given dot-separated keys.
     * 
     * @param project The IntelliJ project instance
     * @param keys List of dot-separated translation keys (e.g., ["common", "button", "save"])
     * @return List of JSON string literal elements found for the given keys
     */
    fun getTranslationValueStringLiterals(project: Project, keys: List<String>?): List<JsonStringLiteral> {
        if (keys.isNullOrEmpty()) return listOf()
        val jsonAssets = getTranslationJsonFilesFromProject(project)

        return jsonAssets.mapNotNull {
            val jsonTranslationPath = keys.listIterator()


            val jsonFile: JsonFile? = PsiManager.getInstance(project).findFile(it) as JsonFile?
            var jsonValue: JsonValue? = JsonUtil.getTopLevelObject(jsonFile)

            while (jsonValue != null && jsonValue is JsonObject && jsonTranslationPath.hasNext()) {
                jsonValue = jsonValue
                    .findProperty(jsonTranslationPath.next())
                    ?.value
            }

            if (!jsonTranslationPath.hasNext() && jsonValue is JsonStringLiteral) jsonValue else null

        }
    }

    /**
     * Retrieves all translation key-value pairs from the project's translation files.
     * 
     * This method scans all configured translation JSON files and extracts all
     * translation keys along with their corresponding values. The results can be
     * filtered by language if configured in the project settings.
     * 
     * @param project The IntelliJ project instance
     * @return Map of translation keys to their string values
     */
    fun getAllTranslationKeyValue(project: Project): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val jsonAssets = getTranslationJsonFilesFromProject(project)
        val psiManager = PsiManager.getInstance(project)
        val config = NgxTranslateConfigurationStateService.getInstance(project).state
        jsonAssets.filter { if (config.lang != null) it.name.contains(config.lang.toString()) else true }.forEach {
            val psiFile = psiManager.findFile(it) as? JsonFile ?: return@forEach
            extractJsonKeyValue(psiFile, map)
        }
        return map
    }

    /**
     * Retrieves all translation keys from the project's translation files.
     * 
     * This method scans all configured translation JSON files and extracts
     * all available translation keys in dot-separated format.
     * 
     * @param project The IntelliJ project instance
     * @return List of all available translation keys
     */
    fun getAllTranslationKeys(project: Project): List<String> {
        val jsonAssets = getTranslationJsonFilesFromProject(project)
        val psiManager = PsiManager.getInstance(project)
        return jsonAssets.flatMap {
            val psiFile = psiManager.findFile(it) as? JsonFile ?: return@flatMap emptyList()
            extractJsonKeys(psiFile)
        }
    }

    /**
     * Gets all translation JSON files from the configured i18n paths in the project.
     * 
     * This method retrieves the list of JSON files from all directories configured
     * in the project's translation settings.
     * 
     * @param project The IntelliJ project instance
     * @return List of virtual files representing translation JSON files
     */
    fun getTranslationJsonFilesFromProject(project: Project): List<VirtualFile> {
        return NgxTranslateConfigurationStateService
            .getInstance(project).state.i18nPaths
            .map { getTranslationJsonFilesFromDirPath(it) }
            .flatten()
    }

    /**
     * Retrieves all JSON translation files from a specific directory path.
     * 
     * @param path The absolute path to the directory to scan
     * @return List of JSON files found in the directory
     */
    private fun getTranslationJsonFilesFromDirPath(path: String): List<VirtualFile> {
        return VirtualFileManager.getInstance().runCatching {
            findFileByNioPath(Path(path))
                .takeIf { it?.isDirectory ?: false }
        }
            .getOrNull()
            ?.children
            ?.filter { it.fileType == JsonFileType.INSTANCE }
            ?: listOf()
    }


    /**
     * Extracts all translation keys from a JSON file in dot-separated format.
     * 
     * This method recursively traverses the JSON object structure and builds
     * dot-separated keys for nested objects (e.g., "user.profile.name").
     * 
     * @param jsonFile The JSON file to extract keys from
     * @return List of dot-separated translation keys
     */
    private fun extractJsonKeys(jsonFile: JsonFile): List<String> {
        val result = mutableListOf<String>()
        val root = jsonFile.topLevelValue as? JsonObject ?: return result

        fun traverse(prefix: String, obj: JsonObject) {
            obj.propertyList.forEach { prop ->
                val key = if (prefix.isEmpty()) prop.name else "$prefix.${prop.name}"
                when (val value = prop.value) {
                    is JsonObject -> traverse(key, value)
                    else -> result.add(key)
                }
            }
        }

        traverse("", root)
        return result
    }

    /**
     * Extracts all translation key-value pairs from a JSON file.
     * 
     * This method recursively traverses the JSON object structure and extracts
     * all key-value pairs, handling nested objects by creating dot-separated keys.
     * 
     * @param jsonFile The JSON file to extract data from
     * @param map Mutable map to populate with key-value pairs
     */
    private fun extractJsonKeyValue(jsonFile: JsonFile, map: MutableMap<String, String>) {

        val root = jsonFile.topLevelValue as? JsonObject ?: return

        fun traverse(prefix: String, obj: JsonObject) {
            obj.propertyList.forEach { prop ->
                val key = if (prefix.isEmpty()) prop.name else "$prefix.${prop.name}"
                when (val value = prop.value) {
                    is JsonObject -> traverse(key, value)
                    else -> map[key] = value?.text?.trim('"') ?: ""
                }
            }
        }
        traverse("", root)
    }
}
