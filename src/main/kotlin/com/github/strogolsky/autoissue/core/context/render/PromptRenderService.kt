package com.github.strogolsky.autoissue.core.context.render

import com.github.strogolsky.autoissue.core.masking.ContentMasker
import com.intellij.openapi.components.Service

/**
 * Service for rendering prompts to send to the LLM and masking sensitive information.
 *
 * Acts as a facade to:
 * - Build prompts in a specific format (XML, Markdown, plain text) using PromptRenderer
 * - Mask sensitive data (API keys, credentials, etc.) before sending to the AI
 *
 * Initialized during plugin startup by AutoIssueSetupTool based on configuration.
 * Used throughout the application when sending context to the LLM.
 *
 * Project-level service (separate instance per open project).
 */
@Service(Service.Level.PROJECT)
class PromptRenderService {
    private var renderer: PromptRenderer? = null
    private var masker: ContentMasker = ContentMasker { it }

    /**
     * Initializes the service with a specific renderer and masker.
     * Called during plugin startup.
     *
     * @param renderer The prompt renderer (determines output format: XML, Markdown, etc.)
     * @param masker The content masker for removing sensitive information
     */
    fun initialize(
        renderer: PromptRenderer,
        masker: ContentMasker,
    ) {
        this.renderer = renderer
        this.masker = masker
    }

    /**
     * Builds a prompt using the given builder block and masks sensitive data.
     *
     * Uses the initialized renderer to convert the builder calls into the appropriate
     * format, then applies content masking before returning.
     *
     * @param block Lambda that builds the prompt by calling instruction(), section(), components()
     * @return The fully rendered and masked prompt ready to send to the LLM
     */
    fun buildPrompt(block: PromptBuilder.() -> Unit): String {
        val r = renderer ?: error("PromptRenderService was not initialized by AutoIssueSetupTool")
        return masker.mask(r.buildPrompt(block))
    }

    /**
     * Masks sensitive information from the given text.
     * Applied to file content before sending to the AI to protect credentials, keys, etc.
     *
     * @param text The text to mask
     * @return The masked text with sensitive patterns removed or redacted
     */
    fun mask(text: String): String = masker.mask(text)
}
