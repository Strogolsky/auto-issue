package com.github.strogolsky.autoissue.core.masking

/**
 * One masking rule for removing sensitive information.
 *
 * The regex MUST have exactly one capturing group wrapping the sensitive value.
 * Only the captured group is replaced with the replacement string, keeping surrounding
 * syntax (key name, quotes, prefix) visible for context. This preserves code structure
 * while redacting secrets.
 *
 * Example:
 * - Pattern: Regex("""token\s*=\s*"([^"]+)"""") with replacement "****"
 * - Input: `token = "abc123xyz"`
 * - Output: `token = "****"`  (the key and quotes are preserved)
 *
 * @param regex Regex with exactly one capturing group wrapping the sensitive value
 * @param replacement String to replace captured sensitive values with (default: "****")
 */
data class MaskingPattern(
    val regex: Regex,
    val replacement: String = "****",
)
