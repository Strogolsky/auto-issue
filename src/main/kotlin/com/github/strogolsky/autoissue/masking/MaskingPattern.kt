package com.github.strogolsky.autoissue.masking

/**
 * One masking rule. The [regex] must have exactly one capturing group wrapping
 * the sensitive value. The masker replaces only that group with [replacement],
 * so surrounding syntax (key name, quotes, prefix) stays visible in the prompt.
 */
data class MaskingPattern(
    val regex: Regex,
    val replacement: String = "****",
)
