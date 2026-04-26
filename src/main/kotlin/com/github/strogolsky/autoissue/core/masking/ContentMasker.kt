package com.github.strogolsky.autoissue.core.masking

fun interface ContentMasker {
    fun mask(text: String): String
}
