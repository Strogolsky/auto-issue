package com.github.strogolsky.autoissue.plugin.startup

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LangfuseConfigLoaderTest {
    private val keys =
        listOf(
            "autoissue.langfuse.url",
            "autoissue.langfuse.public-key",
            "autoissue.langfuse.secret-key",
        )

    @After
    fun tearDown() {
        keys.forEach { System.clearProperty(it) }
    }

    @Test
    fun should_ReturnConfig_When_AllPropertiesAreValid() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        System.setProperty("autoissue.langfuse.url", "https://cloud.langfuse.com")
        System.setProperty("autoissue.langfuse.public-key", "pk-123")
        System.setProperty("autoissue.langfuse.secret-key", "sk-456")

        // 2. ACT
        val config = LangfuseConfigLoader.load()

        // 3. ASSERT
        assertEquals("https://cloud.langfuse.com", config?.url)
        assertEquals("pk-123", config?.publicKey)
        assertEquals("sk-456", config?.secretKey)
    }

    @Test
    fun should_ReturnNull_When_UrlIsMissing() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        System.setProperty("autoissue.langfuse.public-key", "pk-123")
        System.setProperty("autoissue.langfuse.secret-key", "sk-456")

        // 2. ACT
        val result = LangfuseConfigLoader.load()

        // 3. ASSERT
        assertNull(result)
    }

    @Test
    fun should_ReturnNull_When_UrlIsBlank() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        System.setProperty("autoissue.langfuse.url", "   ")
        System.setProperty("autoissue.langfuse.public-key", "pk-123")
        System.setProperty("autoissue.langfuse.secret-key", "sk-456")

        // 2. ACT
        val result = LangfuseConfigLoader.load()

        // 3. ASSERT
        assertNull(result)
    }

    @Test
    fun should_ReturnNull_When_PublicKeyIsEmpty() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        System.setProperty("autoissue.langfuse.url", "https://cloud.langfuse.com")
        System.setProperty("autoissue.langfuse.public-key", "")
        System.setProperty("autoissue.langfuse.secret-key", "sk-456")

        // 2. ACT
        val result = LangfuseConfigLoader.load()

        // 3. ASSERT
        assertNull(result)
    }
}
