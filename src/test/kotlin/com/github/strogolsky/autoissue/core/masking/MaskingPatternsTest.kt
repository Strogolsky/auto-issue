package com.github.strogolsky.autoissue.core.masking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskingPatternsTest {
    @Test
    fun should_ContainPatterns_When_AllListIsAccessed() {
        // --- TEST FLOW ---
        // 1. ARRANGE & ACT: Access the ALL list.
        val patterns = MaskingPatterns.ALL

        // 2. ASSERT: Verify the list is populated.
        assertTrue("Patterns list should not be empty", patterns.isNotEmpty())
    }

    @Test
    fun should_HaveNonEmptyRegex_When_PatternIsDefined() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Get all predefined patterns.
        val patterns = MaskingPatterns.ALL

        // 2. ACT & ASSERT: Iterate through patterns and verify regex is valid and not empty.
        patterns.forEach { pattern ->
            assertTrue(
                "Regex should not be empty for pattern ${pattern.regex.pattern}",
                pattern.regex.pattern.isNotEmpty(),
            )
        }
    }

    @Test
    fun should_HaveDefaultReplacement_When_NotExplicitlyOverridden() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Select patterns that shouldn't have custom replacements.
        val defaultReplacementPatterns =
            listOf(
                MaskingPatterns.BEARER_TOKEN,
                MaskingPatterns.PRIVATE_KEY_ASSIGNMENT,
                MaskingPatterns.PASSWORD_ASSIGNMENT,
            )

        // 2. ACT & ASSERT: Check the replacement property for each.
        defaultReplacementPatterns.forEach { pattern ->
            assertEquals(
                "Default replacement should be ****",
                "****",
                pattern.replacement,
            )
        }
    }
}
