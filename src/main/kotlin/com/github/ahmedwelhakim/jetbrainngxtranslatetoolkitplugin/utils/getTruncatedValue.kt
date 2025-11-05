package com.github.ahmedwelhakim.jetbrainngxtranslatetoolkitplugin.utils

const val TRANSLATION_TEXT_LENGTH = 40
fun getTruncatedValue(text: String?): String? {
    return text?.take(TRANSLATION_TEXT_LENGTH)?.let { if(text.length > TRANSLATION_TEXT_LENGTH) "$it..." else it }
}