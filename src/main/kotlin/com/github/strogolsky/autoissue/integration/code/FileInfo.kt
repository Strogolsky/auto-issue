package com.github.strogolsky.autoissue.integration.code

data class FileInfo(
    val content: String,
    val truncated: Boolean,
    val maxChars: Int,
)
