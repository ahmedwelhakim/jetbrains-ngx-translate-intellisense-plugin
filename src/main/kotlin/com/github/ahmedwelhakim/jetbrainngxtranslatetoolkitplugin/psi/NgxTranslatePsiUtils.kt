package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.psi

import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.services.NgxTranslateConfigurationStateService
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

object NgxTranslatePsiUtils {
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

    fun getAllTranslationKeys(project: Project): List<String> {
        val jsonAssets = getTranslationJsonFilesFromProject(project)
        val psiManager = PsiManager.getInstance(project)
        return jsonAssets.flatMap {
            val psiFile = psiManager.findFile(it) as? JsonFile ?: return@flatMap emptyList()
            extractJsonKeys(psiFile)
        }
    }

    fun getTranslationJsonFilesFromProject(project: Project): List<VirtualFile> {
        return NgxTranslateConfigurationStateService
            .getInstance(project).state.i18nPaths
            .map { getTranslationJsonFilesFromDirPath(it) }
            .flatten()
    }

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