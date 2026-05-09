package com.github.strogolsky.autoissue.core.masking

/**
 * Implementation of ContentMasker that uses regex patterns to identify and redact sensitive data.
 *
 * Applies a list of MaskingPattern rules sequentially to the input text. Each pattern
 * identifies sensitive content via regex and replaces the captured group with a redaction.
 *
 * The regex patterns preserve surrounding context (quotes, key names, etc.) to maintain
 * code structure and readability even with redacted values.
 */
class RegexContentMasker(private val patterns: List<MaskingPattern>) : ContentMasker {
    /**
     * Masks all sensitive patterns in the given text.
     *
     * Iterates through the masking patterns and applies each one to the text.
     * Only the captured group (group 1) is replaced; surrounding text is preserved.
     *
     * @param text The text to mask
     * @return The masked text with sensitive values replaced
     */
    override fun mask(text: String): String {
        var result = text
        for (pattern in patterns) {
            result =
                pattern.regex.replace(result) { match ->
                    val group = match.groups[1] ?: return@replace match.value

                    val localStart = group.range.first - match.range.first
                    val localEnd = group.range.last - match.range.first + 1

                    match.value.replaceRange(localStart, localEnd, pattern.replacement)
                }
        }
        return result
    }
}
