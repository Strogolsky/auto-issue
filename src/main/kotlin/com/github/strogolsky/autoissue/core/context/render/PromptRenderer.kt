package com.github.strogolsky.autoissue.core.context.render

import com.github.strogolsky.autoissue.core.context.components.ContextComponent

/**
 * Interface for rendering prompts in a specific format for the LLM.
 *
 * Different renderers format prompts differently:
 * - XmlPromptRenderer: Formats as XML elements (good for structured parsing)
 * - MarkdownPromptRenderer: Formats as Markdown (readable, with headers and lists)
 * - PlainTextPromptRenderer: Formats as plain text (simple, no markup)
 *
 * Renderers are discovered via extension points and registered in PromptRendererRegistry.
 * The selected renderer is configured during plugin startup based on settings.
 *
 * Implementations:
 * - XmlPromptRenderer
 * - MarkdownPromptRenderer
 * - PlainTextPromptRenderer
 */
interface PromptRenderer {
    /**
     * Returns the unique identifier for this renderer format.
     * Used to select which renderer to use based on configuration.
     *
     * @return Renderer ID (e.g., "xml", "markdown", "plain")
     */
    fun rendererKey(): String

    /**
     * Renders a context component into the prompt format.
     * Called for each component added to the prompt.
     *
     * @param component The context component to render
     * @return The rendered component as a string in this renderer's format
     */
    fun renderComponent(component: ContextComponent): String

    /**
     * Builds a complete prompt by executing the builder block and rendering all parts.
     * Creates a PromptBuilder, executes the block (which calls instruction(), section(), components()),
     * and renders everything into the final format.
     *
     * @param block Lambda that builds the prompt structure
     * @return The fully rendered prompt in this renderer's format
     */
    fun buildPrompt(block: PromptBuilder.() -> Unit): String
}
