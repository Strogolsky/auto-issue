package com.github.strogolsky.autoissue.agent.context

import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class RendererFactoryHolder {
    lateinit var factory: RendererFactory
}
