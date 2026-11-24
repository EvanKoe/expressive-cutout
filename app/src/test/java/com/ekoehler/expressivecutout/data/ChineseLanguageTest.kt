package com.ekoehler.expressivecutout.data

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ChineseLanguageTest {
    private val tags = listOf("en", "fr-FR", "tr", "zh-Hans")

    @Test fun `picker shows an explicit Simplified Chinese name`() {
        assertEquals("简体中文", AppLanguage("zh-Hans", Locale.forLanguageTag("zh-Hans")).displayName)
    }

    @Test fun `Chinese regional and script tags resolve to the bundled translation`() {
        for (tag in listOf("zh", "zh-CN", "zh-Hans", "zh-Hans-CN", "zh-SG")) {
            assertEquals(tag, "zh-Hans", AppLanguages.normaliseTag(tag, tags))
        }
    }

    @Test fun `existing languages and unknown fallback retain upstream behavior`() {
        assertEquals("en", AppLanguages.normaliseTag("en-GB", tags))
        assertEquals("fr-FR", AppLanguages.normaliseTag("fr-CA", tags))
        assertEquals("tr", AppLanguages.normaliseTag("tr-TR", tags))
        assertEquals("en", AppLanguages.normaliseTag("de", tags))
        assertEquals("en", AppLanguages.normaliseTag("", tags))
    }
}
