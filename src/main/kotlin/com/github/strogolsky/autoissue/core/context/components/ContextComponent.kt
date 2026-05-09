package com.github.strogolsky.autoissue.core.context.components

/**
 * Base interface for context components.
 *
 * Context components represent different types of information provided to the AI agent
 * to help it generate better issues. All context information is sealed under this interface.
 *
 * Examples of implementations:
 * - IssueInstruction: The user's instruction
 * - JiraProjectMetadata: Project structure and available fields
 * - FileContext: Code content and file information
 *
 * The sealed interface ensures type safety and exhaustive pattern matching
 * when processing context components.
 */
sealed interface ContextComponent
