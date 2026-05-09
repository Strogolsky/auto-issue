package com.github.strogolsky.autoissue.core.agent.tools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("SymbolSearchResponse")
data class SymbolSearchResponse(
    val query: String,
    val results: List<String>,
) : ToolResponse
