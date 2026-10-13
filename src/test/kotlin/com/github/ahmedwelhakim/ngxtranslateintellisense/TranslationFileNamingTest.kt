package com.github.ahmedwelhakim.ngxtranslateintellisense

import com.github.ahmedwelhakim.ngxtranslateintellisense.common.NgxTranslateUtils
import com.github.ahmedwelhakim.ngxtranslateintellisense.psi.NgxTranslatePsiUtils
import com.github.ahmedwelhakim.ngxtranslateintellisense.psi.NgxTranslatePsiUtils.TranslationKeyMismatchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationFileNamingTest {
    @Test
    fun recognizesPlainAndSectionPrefixedLocales() {
        assertEquals("" to "ru", NgxTranslateUtils.translationFileParts("ru.json"))
        assertEquals("" to "en-US", NgxTranslateUtils.translationFileParts("en-US.json"))
        assertEquals("settings" to "ru", NgxTranslateUtils.translationFileParts("settings.ru.json"))
        assertEquals(
            "analysis-settings" to "en-US",
            NgxTranslateUtils.translationFileParts("analysis-settings.en-US.json")
        )
        assertNull(NgxTranslateUtils.translationFileParts("settings.json"))
        assertNull(NgxTranslateUtils.translationFileParts("settings.ru.json.bak"))
    }

    @Test
    fun comparesKeysOnlyWithinTheSameSection() {
        val result = NgxTranslatePsiUtils.validateTranslationKeySets(
            mapOf(
                "ru.json" to setOf("brand.name"),
                "en.json" to setOf("brand.name"),
                "settings.ru.json" to setOf("settings.title"),
                "settings.en.json" to setOf("settings.title")
            )
        )

        assertTrue(result.isValid)
        assertTrue(result.mismatchDetails.isEmpty())
    }

    @Test
    fun reportsMissingKeysInTheMatchingSectionOnly() {
        val result = NgxTranslatePsiUtils.validateTranslationKeySets(
            mapOf(
                "ru.json" to setOf("brand.name"),
                "en.json" to setOf("brand.name"),
                "settings.ru.json" to setOf("settings.title", "settings.save"),
                "settings.en.json" to setOf("settings.title")
            )
        )

        assertFalse(result.isValid)
        assertEquals(setOf("settings.ru.json", "settings.en.json"), result.mismatchDetails.keys)
        assertTrue(result.mismatchDetails.getValue("settings.en.json").any {
            it.key == "settings.save" && it.type == TranslationKeyMismatchType.MISSING
        })
    }
}
