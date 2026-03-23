package com.github.strogolsky.autoissue.context

data class TestEnvironment (
    val mockFileName: String,
    val mockSelectedCode: String
) : ContextEnvironment