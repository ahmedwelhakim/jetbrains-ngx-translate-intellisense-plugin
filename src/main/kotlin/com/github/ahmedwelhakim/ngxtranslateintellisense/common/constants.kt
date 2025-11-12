package com.github.ahmedwelhakim.ngxtranslateintellisense.common

/**
 * Object containing constant values used throughout the NgxTranslateIntellisense plugin.
 *
 * This object centralizes configuration constants such as directory exclusion patterns
 * and file naming conventions for translation files.
 */
object NgxTranslateConstants {

    /**
     * Set of directory names to exclude when scanning for translation files.
     *
     * These directories are typically not relevant for i18n file discovery and are
     * skipped during automatic path detection to improve performance and avoid false positives.
     */
    val EXCLUDED_DIRS_IN_PATH = setOf(
        "node_modules",
        ".git",
        "dist",
        "build",
        "out",
        ".idea",
        ".gradle"
    )

    /**
     * Regular expression pattern for matching locale-specific JSON translation files.
     *
     * This pattern matches files with names like:
     * - "en.json" (language code only)
     * - "en-US.json" (language code with country code)
     *
     * The pattern is case-insensitive to handle various naming conventions.
     */
    val LOCALE_PATTERN = Regex("^[a-z]{2}(-[A-Z]{2})?\\.json$", RegexOption.IGNORE_CASE)
}
