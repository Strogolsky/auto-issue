package com.github.strogolsky.autoissue.core.masking

import org.junit.Assert.assertEquals
import org.junit.Test

class RegexContentMaskerTest {
    private val masker = RegexContentMasker(MaskingPatterns.ALL)

    @Test
    fun should_ReturnOriginalString_When_InputIsEmptyOrHasNoSecrets() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Prepare text without any sensitive data and empty strings.
        val emptyText = ""
        val normalText = "Hello world"
        val emptyMasker = RegexContentMasker(emptyList())

        // 2. ACT: Process the text through the masker.
        val emptyResult = masker.mask(emptyText)
        val normalResult = masker.mask(normalText)
        val noPatternsResult = emptyMasker.mask("secret = '123'")

        // 3. ASSERT: Verify that nothing was changed.
        assertEquals("", emptyResult)
        assertEquals("Hello world", normalResult)
        assertEquals("secret = '123'", noPatternsResult)
    }

    @Test
    fun should_MaskBearerToken_When_TextContainsAuthorizationHeader() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Prepare a multiline HTTP request containing a Bearer token.
        val multilineRequest =
            """
            GET /api HTTP/1.1
            Authorization: Bearer secret-token-xyz
            Host: example.com
            """.trimIndent()

        // 2. ACT: Mask the text.
        val result = masker.mask(multilineRequest)

        // 3. ASSERT: Verify only the token is replaced, while the 'Bearer' prefix remains.
        val expected =
            """
            GET /api HTTP/1.1
            Authorization: Bearer ****
            Host: example.com
            """.trimIndent()
        assertEquals(expected, result)
    }

    @Test
    fun should_MaskCredentials_When_TextContainsDatabaseUris() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Prepare JDBC, MongoDB, and Redis connection strings.
        val pgUri = "spring.datasource.url=jdbc:postgresql://user:pass@localhost:5432/db"
        val mongoUri = "URI = mongodb+srv://user:pass@host/db?retryWrites=true"
        val redisUri = "cache=redis://:mypassword@localhost:6379"

        // 2. ACT: Mask the connection strings.
        val pgResult = masker.mask(pgUri)
        val mongoResult = masker.mask(mongoUri)
        val redisResult = masker.mask(redisUri)

        // 3. ASSERT: Verify credentials are masked but protocol prefixes remain intact.
        assertEquals("spring.datasource.url=jdbc:<masked>", pgResult)
        assertEquals("URI = mongodb://<masked>", mongoResult)
        assertEquals("cache=redis://<masked>", redisResult)
    }

    @Test
    fun should_MaskSecretValue_When_TextContainsKeyValueAssignments() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Prepare various variable assignments with sensitive names.
        val privateKey = "private_key = \"MIIEvQIBADANBgkq\""
        val password = "password = 'myS3cr3t'"
        val envLookup = "System.getenv(\"SECRET_KEY\")"

        // 2. ACT: Mask the assignments.
        val keyResult = masker.mask(privateKey)
        val passResult = masker.mask(password)
        val envResult = masker.mask(envLookup)

        // 3. ASSERT: Verify the keys stay readable, but the assigned values are masked.
        assertEquals("private_key = \"****\"", keyResult)
        assertEquals("password = '****'", passResult)
        assertEquals("System.getenv(\"****\")", envResult)
    }

    @Test
    fun should_MaskAllSecrets_When_TextContainsMultipleSecrets() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Prepare a string with multiple different secrets.
        val input = "apiKey = \"123\" and password = \"456\""

        // 2. ACT: Process the string.
        val result = masker.mask(input)

        // 3. ASSERT: Verify both secrets are replaced successfully.
        assertEquals("apiKey = \"****\" and password = \"****\"", result)
    }

    @Test
    fun should_NotMaskText_When_TextContainsFalsePositives() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Prepare text with sensitive words but without actual assignments (e.g., docs).
        val falsePositive1 = "The password policy requires 8 chars"
        val falsePositive2 = "// TODO: add token support"

        // 2. ACT: Process the false positives.
        val result1 = masker.mask(falsePositive1)
        val result2 = masker.mask(falsePositive2)

        // 3. ASSERT: Verify the text was not modified.
        assertEquals(falsePositive1, result1)
        assertEquals(falsePositive2, result2)
    }

    @Test
    fun should_MaskOnlyValue_When_KeyEqualsValue() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Prepare an edge-case string where the variable name equals its value.
        val edgeCaseInput = "password = \"password\""

        // 2. ACT: Mask the string.
        val result = masker.mask(edgeCaseInput)

        // 3. ASSERT: Verify the key is intact and only the value is masked.
        assertEquals("password = \"****\"", result)
    }
}
