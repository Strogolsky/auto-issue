package com.github.strogolsky.autoissue.integration.code

data class MethodContext(
    val name: String,
    val signature: String,
    val body: String,
)