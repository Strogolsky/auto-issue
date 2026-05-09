package com.github.strogolsky.autoissue.core.masking

class RegexContentMasker(private val patterns: List<MaskingPattern>) : ContentMasker {
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
