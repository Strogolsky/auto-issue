package com.github.strogolsky.autoissue.core.agent.strategy

import com.github.strogolsky.autoissue.core.input.AgentInput

data class AnalysisContext(
    val originalInput: AgentInput,
    val analysisText: String,
)