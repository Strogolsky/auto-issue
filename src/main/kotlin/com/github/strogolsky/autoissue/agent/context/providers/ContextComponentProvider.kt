package com.github.strogolsky.autoissue.agent.context.providers

import com.github.strogolsky.autoissue.agent.context.ContextEnvironment
import com.github.strogolsky.autoissue.agent.context.components.ContextComponent

interface ContextComponentProvider {
    suspend fun provide(env: ContextEnvironment): ContextComponent?
}
