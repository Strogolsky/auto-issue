package com.github.strogolsky.autoissue.core.masking

/**
 * Functional interface for masking sensitive information from text.
 *
 * Implementations remove or redact sensitive data like API keys, passwords, and credentials
 * before sending source code or prompts to the LLM.
 *
 * Primary implementation: RegexContentMasker
 */
fun interface ContentMasker {
    /**
     * Masks sensitive information in the given text.
     * Returns a version of the text with sensitive data redacted or replaced.
     *
     * @param text The text to mask
     * @return The masked text with sensitive patterns replaced
     */
    fun mask(text: String): String
}
