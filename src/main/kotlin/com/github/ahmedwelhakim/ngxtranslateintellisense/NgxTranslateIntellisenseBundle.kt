package com.github.ahmedwelhakim.ngxtranslateintellisense

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

/**
 * Bundle class for managing internationalized messages and localization.
 * 
 * This class provides access to localized strings used throughout the NgxTranslateIntellisense plugin.
 * It extends DynamicBundle to support hot-reloading of localization properties during development.
 */
@NonNls
private const val BUNDLE = "messages.NgxTranslateIntellisenseBundle"

/**
 * Object that provides access to localized messages for the NgxTranslateIntellisense plugin.
 * 
 * Use this object to retrieve localized strings by their keys from the resource bundle.
 * Supports both immediate and lazy message retrieval.
 */
object NgxTranslateIntellisenseBundle : DynamicBundle(BUNDLE) {

    /**
     * Retrieves a localized message by key with optional parameters.
     * 
     * @param key The property key in the resource bundle
     * @param params Optional parameters to format into the message
     * @return The localized message string
     */
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getMessage(key, *params)

    /**
     * Creates a lazy message pointer that resolves the message when accessed.
     * 
     * This method is useful for performance optimization when the message might not be needed immediately.
     * 
     * @param key The property key in the resource bundle
     * @param params Optional parameters to format into the message
     * @return A lazy message provider
     */
    @Suppress("unused")
    @JvmStatic
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)
}
