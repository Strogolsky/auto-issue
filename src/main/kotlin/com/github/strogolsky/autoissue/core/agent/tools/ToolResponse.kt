package com.github.strogolsky.autoissue.core.agent.tools

import kotlinx.serialization.Serializable

/**
 * Base interface for tool responses.
 *
 * All responses from agent tools implement this interface.
 * Implementations include:
 * - Successful responses with results (class lists, file contents, etc.)
 * - Error responses indicating tool failures
 *
 * Sealed interface ensures type safety and exhaustive when clauses.
 */
@Serializable
sealed interface ToolResponse
