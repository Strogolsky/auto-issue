package com.github.strogolsky.autoissue.context

interface ContextComponentProvider {
    suspend fun provide(env: ContextEnvironment): ContextComponent?
}
