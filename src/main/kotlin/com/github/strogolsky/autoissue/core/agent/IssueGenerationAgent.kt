package com.github.strogolsky.autoissue.core.agent

/**
 * Generic interface for AI agents that generate issues.
 *
 * Implementations process input (typically containing user instructions and context)
 * and produce output (typically JIRA issue candidates).
 *
 * The generate function is suspend to support asynchronous AI API calls.
 *
 * @param I The input type containing instruction and context information
 * @param O The output type containing the generated issue candidate
 */
interface IssueGenerationAgent<I, O> {
    /**
     * Generates an issue based on the provided input.
     *
     * This method communicates with an AI LLM service to generate an issue
     * based on the user's instruction and gathered project context.
     *
     * @param input The input containing instruction and context components
     * @return The generated issue candidate
     */
    suspend fun generate(input: I): O
}
