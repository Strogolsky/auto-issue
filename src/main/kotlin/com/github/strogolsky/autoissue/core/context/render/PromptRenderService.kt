package com.github.strogolsky.autoissue.core.context.render

import com.github.strogolsky.autoissue.core.masking.ContentMasker
import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class PromptRenderService {
    private var renderer: PromptRenderer? = null
    private var masker: ContentMasker = ContentMasker { it }

    fun initialize(
        renderer: PromptRenderer,
        masker: ContentMasker,
    ) {
        this.renderer = renderer
        this.masker = masker
    }

    fun buildPrompt(block: PromptBuilder.() -> Unit): String {
        val r = renderer ?: error("PromptRenderService was not initialized by AutoIssueSetupTool")
        return masker.mask(r.buildPrompt(block))
    }
}
