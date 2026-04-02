package com.github.strogolsky.autoissue.agent.context.components

data class FileContextComponent(
    val fileName: String,
    val language: String,
    val imports: List<String>,
    val className: String?,
    val classFields: List<String>,
    val methodSignature: String?,
    val methodBody: String,
) : ContextComponent
