package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.utils

import com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.services.NgxTranslateConfigurationStateService
import com.intellij.openapi.project.Project


fun getTruncatedValue(text: String?, project: Project): String? {
    val config = project.getService(NgxTranslateConfigurationStateService::class.java)
    val length = config.state.inlayHintLength
    return text?.take(length)
        ?.let { if (text.length > length) "$it..." else it }
}