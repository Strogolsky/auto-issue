package com.github.strogolsky.autoissue.core.masking

class RegexContentMasker(private val patterns: List<MaskingPattern>) : ContentMasker {
    override fun mask(text: String): String {
        var result = text
        for (pattern in patterns) {
            result =
                pattern.regex.replace(result) { match ->
                    val group = match.groups[1] ?: return@replace match.value
                    match.value.replace(group.value, pattern.replacement)
                }
        }
        return result
    }
}
