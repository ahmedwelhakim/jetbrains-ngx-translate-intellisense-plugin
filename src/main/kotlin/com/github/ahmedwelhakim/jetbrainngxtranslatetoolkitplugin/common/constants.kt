package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.common

object NgxTranslateConstants {
    val EXCLUDED_DIRS_IN_PATH = setOf(
        "node_modules",
        ".git",
        "dist",
        "build",
        "out",
        ".idea",
        ".gradle"
    )
    val LOCALE_PATTERN = Regex("^[a-z]{2}(-[A-Z]{2})?\\.json$", RegexOption.IGNORE_CASE)
}