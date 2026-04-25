package com.github.strogolsky.autoissue.masking

fun interface ContentMasker {
    fun mask(text: String): String
}
