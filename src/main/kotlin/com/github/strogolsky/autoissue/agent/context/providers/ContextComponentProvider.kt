package com.github.strogolsky.autoissue.agent.context.providers

import com.github.strogolsky.autoissue.agent.context.components.ContextComponent
import com.github.strogolsky.autoissue.agent.context.ContextEnvironment

interface ContextComponentProvider {
    suspend fun provide(env: ContextEnvironment): ContextComponent?
}