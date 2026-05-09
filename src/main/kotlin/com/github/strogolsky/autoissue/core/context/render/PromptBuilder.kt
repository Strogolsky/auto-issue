package com.github.strogolsky.autoissue.core.context.render

import com.github.strogolsky.autoissue.core.context.components.ContextComponent

/**
 * Interface for building prompts to send to the LLM.
 *
 * Implementations receive instructions, context sections, and context components
 * in a structured way, then format them into the appropriate LLM prompt format
 * (XML, Markdown, plain text, etc.).
 *
 * Used by PromptRenderService to construct the complete prompt that will be
 * sent to the AI agent for issue generation.
 */
interface PromptBuilder {
    /**
     * Adds the main instruction to the prompt.
     * Typically describes what the LLM should do (e.g., "Generate a JIRA issue...").
     *
     * @param text The instruction text
     */
    fun instruction(text: String)

    /**
     * Adds a labeled section with content to the prompt.
     * Used to structure information by topic (e.g., "File Context", "JIRA Metadata").
     *
     * @param title The section header
     * @param content The section body content
     */
    fun section(
        title: String,
        content: String,
    )

    /**
     * Adds context components to the prompt.
     * Each component is formatted and included in the final prompt sent to the LLM.
     *
     * @param components List of context components to include
     */
    fun components(components: List<ContextComponent>)
}
