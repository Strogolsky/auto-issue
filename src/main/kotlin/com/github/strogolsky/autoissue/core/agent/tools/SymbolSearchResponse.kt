package com.github.strogolsky.autoissue.core.agent.tools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from SearchSymbolTool containing symbols matching a search query.
 *
 * Provides the AI agent with a list of symbols (classes, functions, interfaces) that match
 * a search query. Uses IntelliJ's symbol index for fast case-insensitive partial matching.
 * Each result is a fully-qualified symbol name that can be located with ReadFileContentTool.
 *
 * @param query The search query used (e.g., "UserValidator" or "Validator")
 * @param results List of fully-qualified symbol names matching the query
 *                (e.g., ["com.example.auth.UserValidator", "com.example.validation.FieldValidator"])
 */
@Serializable
@SerialName("SymbolSearchResponse")
data class SymbolSearchResponse(
    val query: String,
    val results: List<String>,
) : ToolResponse
