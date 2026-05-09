package com.github.strogolsky.autoissue.integration.code

data class DetailedFileInfo(
    val fileName: String,
    val language: String,
    val imports: List<String>,
    val enclosingClass: ClassInfo?,
    val enclosingMethod: MethodInfo?,
    val surroundingText: String,
)
