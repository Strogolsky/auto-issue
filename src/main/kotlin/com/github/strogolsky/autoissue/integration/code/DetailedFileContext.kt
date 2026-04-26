package com.github.strogolsky.autoissue.integration.code

data class DetailedFileContext(
    val fileName: String,
    val language: String,
    val imports: List<String>,
    val enclosingClass: ClassContext?,
    val enclosingMethod: MethodContext?,
    val surroundingText: String,
)
